package com.yach.kinetix.frontofhouse.domain

import com.yach.kinetix.backoffice.data.dao.AuditLogDao
import com.yach.kinetix.backoffice.data.dao.IngredientDao
import com.yach.kinetix.backoffice.data.dao.ProductDao
import com.yach.kinetix.backoffice.data.dao.RecipeDao
import com.yach.kinetix.backoffice.data.entity.AuditLogEntity
import com.yach.kinetix.core.database.TransactionRunner
import com.yach.kinetix.frontofhouse.data.dao.CustomerDao
import com.yach.kinetix.frontofhouse.data.dao.OrderDao
import com.yach.kinetix.frontofhouse.data.dao.TableDao
import com.yach.kinetix.frontofhouse.data.entity.CustomerAccountMovementEntity
import com.yach.kinetix.frontofhouse.data.entity.CustomerMovementType
import com.yach.kinetix.frontofhouse.data.entity.OrderEntity
import com.yach.kinetix.frontofhouse.data.entity.OrderStatus
import com.yach.kinetix.frontofhouse.data.entity.PaymentMethod
import com.yach.kinetix.frontofhouse.data.entity.TableStatus
import com.yach.kinetix.treasury.data.dao.CashShiftDao
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class ProcessSaleUseCase @Inject constructor(
    private val orderDao: OrderDao,
    private val tableDao: TableDao,
    private val productDao: ProductDao,
    private val ingredientDao: IngredientDao,
    private val recipeDao: RecipeDao,
    private val customerDao: CustomerDao,
    private val cashShiftDao: CashShiftDao,
    private val auditLogDao: AuditLogDao,
    private val transactionRunner: TransactionRunner
) {

    /**
     * Procesa y liquida el pago completo de una orden con deducción atómica de inventario
     * (tanto producto terminado como insumos por receta/escandallo BOM),
     * validación preventiva de stock disponible (allowOutOfStockSale),
     * liberación de mesa, soporte de cuenta corriente (fiar) con validación de límite de crédito,
     * asociación al turno de caja activo y auditoría forense.
     */
    suspend operator fun invoke(
        orderId: UUID,
        paymentMethod: PaymentMethod,
        tipAmount: BigDecimal = BigDecimal.ZERO,
        cashierUserId: UUID
    ): Result<OrderEntity> = runCatching {
        transactionRunner.runInTransaction {
            val orderDetails = orderDao.getOrderWithDetailsSync(orderId)
                ?: throw IllegalArgumentException("Orden con ID $orderId no encontrada")

            val order = orderDetails.order
            if (order.isPaid) {
                throw IllegalStateException("La orden $orderId ya se encuentra liquidada")
            }

            val activeShift = cashShiftDao.getActiveShiftSync()
                ?: throw IllegalStateException("Operación rechazada: No existe un turno de caja abierto")

            val finalTotalWithTip = order.totalAmount.add(tipAmount)

            // 1. Validar y procesar crédito a cliente (Cta. Corriente / Fiar)
            if (paymentMethod == PaymentMethod.CUSTOMER_CREDIT) {
                val customerId = order.customerId
                    ?: throw IllegalStateException("Debe asignar un cliente para cobrar a cuenta corriente")

                val customer = customerDao.getCustomerById(customerId)
                    ?: throw IllegalArgumentException("Cliente asignado no encontrado")

                val newDebt = customer.currentDebt.add(finalTotalWithTip)
                if (customer.creditLimit > BigDecimal.ZERO && newDebt > customer.creditLimit) {
                    throw IllegalStateException("Límite de crédito excedido. Límite: $${customer.creditLimit}, Deuda proyectada: $$newDebt")
                }

                customerDao.adjustDebt(customerId, finalTotalWithTip)
                customerDao.insertMovement(
                    CustomerAccountMovementEntity(
                        id = UUID.randomUUID(),
                        customerId = customerId,
                        orderId = order.id,
                        type = CustomerMovementType.CREDIT_PURCHASE,
                        amount = finalTotalWithTip,
                        notes = "Consumo orden #${order.orderNumber}",
                        registeredByUserId = cashierUserId,
                        timestamp = Instant.now()
                    )
                )
            }

            // 2. Validación preventiva de existencias y deducción atómica de inventario (Productos + Recetas BOM)
            orderDetails.items.forEach { itemWithModifiers ->
                val item = itemWithModifiers.item
                val product = productDao.getProductById(item.productId)
                    ?: throw IllegalArgumentException("Producto '${item.productName}' no encontrado en catálogo")

                // Validación preventiva de stock para producto terminado
                if (!product.allowOutOfStockSale && product.stockQuantity < item.quantity) {
                    throw IllegalStateException("Existencias insuficientes para '${product.name}'. Disponible: ${product.stockQuantity}, requerido: ${item.quantity}")
                }

                // Deducción de stock de producto terminado
                productDao.deductStock(
                    productId = item.productId,
                    deductedStock = item.quantity
                )

                // Validación preventiva y deducción de ingredientes según receta BOM
                val recipeItems = recipeDao.getRecipeForProductSync(item.productId)
                for (recipeItem in recipeItems) {
                    val totalIngredientNeeded = recipeItem.requiredQuantity.multiply(item.quantity)
                    val ingredient = ingredientDao.getIngredientById(recipeItem.ingredientId)

                    if (ingredient != null && !product.allowOutOfStockSale && ingredient.stockQuantity < totalIngredientNeeded) {
                        throw IllegalStateException("Insumo insuficiente '${ingredient.name}' para preparar '${product.name}'. Disponible: ${ingredient.stockQuantity} ${ingredient.unitOfMeasure}")
                    }

                    ingredientDao.deductStock(
                        ingredientId = recipeItem.ingredientId,
                        deductedStock = totalIngredientNeeded
                    )
                }

                orderDao.updateOrderItemPaymentSplit(
                    orderItemId = item.id,
                    isPaid = true,
                    splitGroupId = null
                )
            }

            // 3. Liberación de la mesa en caso de consumo en comedor
            order.tableId?.let { tableId ->
                tableDao.updateTableStatus(
                    tableId = tableId,
                    status = TableStatus.AVAILABLE,
                    currentOrderId = null
                )
            }

            // 4. Actualización de orden
            val updatedOrder = order.copy(
                isPaid = true,
                paidAt = Instant.now(),
                shiftId = activeShift.id,
                paymentMethod = paymentMethod,
                tipAmount = tipAmount,
                totalAmount = finalTotalWithTip,
                status = OrderStatus.DELIVERED,
                updatedAt = Instant.now()
            )

            orderDao.updateOrder(updatedOrder)

            // 5. Bitácora forense de auditoría
            auditLogDao.insertLog(
                AuditLogEntity(
                    userId = cashierUserId,
                    action = "PROCESS_SALE",
                    entityName = "Order",
                    entityId = order.id.toString(),
                    details = "Venta cobrada. Folio: #${order.orderNumber}. Método: $paymentMethod. Total: $$finalTotalWithTip (Base: $${order.baseAmount}, Impuesto: $${order.taxAmount}, Descuento: $${order.discountAmount}). Turno: ${activeShift.id}"
                )
            )

            updatedOrder
        }
    }
}
