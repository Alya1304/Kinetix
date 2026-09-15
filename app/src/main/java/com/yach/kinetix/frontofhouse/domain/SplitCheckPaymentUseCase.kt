package com.yach.kinetix.frontofhouse.domain

import com.yach.kinetix.backoffice.data.dao.AuditLogDao
import com.yach.kinetix.backoffice.data.dao.IngredientDao
import com.yach.kinetix.backoffice.data.dao.ProductDao
import com.yach.kinetix.backoffice.data.dao.RecipeDao
import com.yach.kinetix.backoffice.data.entity.AuditLogEntity
import com.yach.kinetix.core.database.TransactionRunner
import com.yach.kinetix.frontofhouse.data.dao.OrderDao
import com.yach.kinetix.frontofhouse.data.dao.TableDao
import com.yach.kinetix.frontofhouse.data.entity.OrderStatus
import com.yach.kinetix.frontofhouse.data.entity.PaymentMethod
import com.yach.kinetix.frontofhouse.data.entity.TableStatus
import com.yach.kinetix.treasury.data.dao.CashShiftDao
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

data class SplitPaymentResult(
    val splitGroupId: UUID,
    val itemsPaidCount: Int,
    val amountPaid: BigDecimal,
    val isOrderFullyPaid: Boolean
)

class SplitCheckPaymentUseCase @Inject constructor(
    private val orderDao: OrderDao,
    private val tableDao: TableDao,
    private val productDao: ProductDao,
    private val ingredientDao: IngredientDao,
    private val recipeDao: RecipeDao,
    private val cashShiftDao: CashShiftDao,
    private val auditLogDao: AuditLogDao,
    private val transactionRunner: TransactionRunner
) {

    /**
     * Procesa el cobro fraccionado (Split Check) de ítems seleccionados de una cuenta activa,
     * deduciendo atómicamente producto e insumos por receta (BOM) y validando existencias.
     * Si la totalidad de los ítems queda liquidada, finaliza la orden y libera la mesa.
     */
    suspend operator fun invoke(
        orderId: UUID,
        selectedOrderItemIds: List<UUID>,
        paymentMethod: PaymentMethod,
        tipAmount: BigDecimal = BigDecimal.ZERO,
        cashierUserId: UUID
    ): Result<SplitPaymentResult> = runCatching {
        require(selectedOrderItemIds.isNotEmpty()) { "Debe seleccionar al menos un producto a cobrar" }

        transactionRunner.runInTransaction {
            val orderDetails = orderDao.getOrderWithDetailsSync(orderId)
                ?: throw IllegalArgumentException("Orden con ID $orderId no encontrada")

            val activeShift = cashShiftDao.getActiveShiftSync()
                ?: throw IllegalStateException("Operación rechazada: No existe un turno de caja abierto")

            val splitGroupId = UUID.randomUUID()
            var splitTotal = BigDecimal.ZERO
            var itemsPaidCount = 0

            val selectedSet = selectedOrderItemIds.toSet()

            // 1. Validar y cobrar ítems seleccionados
            for (itemWithMods in orderDetails.items) {
                val item = itemWithMods.item
                if (selectedSet.contains(item.id)) {
                    if (item.isPaid) {
                        throw IllegalStateException("El producto ${item.productName} ya había sido pagado previamente")
                    }

                    val product = productDao.getProductById(item.productId)
                        ?: throw IllegalArgumentException("Producto '${item.productName}' no encontrado en catálogo")

                    // Validación preventiva de existencias
                    if (!product.allowOutOfStockSale && product.stockQuantity < item.quantity) {
                        throw IllegalStateException("Existencias insuficientes para '${product.name}'. Disponible: ${product.stockQuantity}")
                    }

                    // Deducción de stock del producto terminado
                    productDao.deductStock(item.productId, item.quantity)

                    // Deducción de ingredientes según receta BOM
                    val recipeItems = recipeDao.getRecipeForProductSync(item.productId)
                    for (recipeItem in recipeItems) {
                        val totalIngredientNeeded = recipeItem.requiredQuantity.multiply(item.quantity)
                        ingredientDao.deductStock(recipeItem.ingredientId, totalIngredientNeeded)
                    }

                    // Marcar ítem como pagado
                    orderDao.updateOrderItemPaymentSplit(
                        orderItemId = item.id,
                        isPaid = true,
                        splitGroupId = splitGroupId
                    )

                    splitTotal = splitTotal.add(item.subtotalWithTax)
                    itemsPaidCount++
                }
            }

            // 2. Verificar si todos los ítems de la orden ya quedaron liquidados
            val unPaidItemsCount = orderDetails.items.count { itemWithMods ->
                val item = itemWithMods.item
                !item.isPaid && !selectedSet.contains(item.id)
            }

            val isFullyPaid = (unPaidItemsCount == 0)

            if (isFullyPaid) {
                // Si ya no quedan ítems por cobrar, cerrar orden y liberar mesa
                val updatedOrder = orderDetails.order.copy(
                    isPaid = true,
                    paidAt = Instant.now(),
                    shiftId = activeShift.id,
                    paymentMethod = paymentMethod,
                    status = OrderStatus.DELIVERED,
                    updatedAt = Instant.now()
                )
                orderDao.updateOrder(updatedOrder)

                orderDetails.order.tableId?.let { tableId ->
                    tableDao.updateTableStatus(
                        tableId = tableId,
                        status = TableStatus.AVAILABLE,
                        currentOrderId = null
                    )
                }
            } else {
                // La mesa continúa ocupada para los comensales restantes
                orderDetails.order.tableId?.let { tableId ->
                    tableDao.updateTableStatus(
                        tableId = tableId,
                        status = TableStatus.OCCUPIED,
                        currentOrderId = orderDetails.order.id
                    )
                }
            }

            val finalPaidWithTip = splitTotal.add(tipAmount)

            auditLogDao.insertLog(
                AuditLogEntity(
                    userId = cashierUserId,
                    action = "SPLIT_CHECK_PAYMENT",
                    entityName = "Order",
                    entityId = orderId.toString(),
                    details = "Cobro fraccionado de $itemsPaidCount ítems por $$finalPaidWithTip. Orden completa liquidada: $isFullyPaid. Turno: ${activeShift.id}"
                )
            )

            SplitPaymentResult(
                splitGroupId = splitGroupId,
                itemsPaidCount = itemsPaidCount,
                amountPaid = finalPaidWithTip,
                isOrderFullyPaid = isFullyPaid
            )
        }
    }
}
