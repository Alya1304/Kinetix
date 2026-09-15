package com.yach.kinetix.frontofhouse.domain

import com.yach.kinetix.backoffice.data.dao.AuditLogDao
import com.yach.kinetix.backoffice.data.dao.ProductDao
import com.yach.kinetix.backoffice.data.entity.AuditLogEntity
import com.yach.kinetix.core.database.TransactionRunner
import com.yach.kinetix.core.finance.DiscountType
import com.yach.kinetix.core.finance.TaxCalculator
import com.yach.kinetix.frontofhouse.data.dao.CustomerDao
import com.yach.kinetix.frontofhouse.data.dao.ModifierDao
import com.yach.kinetix.frontofhouse.data.dao.OrderDao
import com.yach.kinetix.frontofhouse.data.dao.TableDao
import com.yach.kinetix.frontofhouse.data.entity.KdsItemStatus
import com.yach.kinetix.frontofhouse.data.entity.OrderEntity
import com.yach.kinetix.frontofhouse.data.entity.OrderItemEntity
import com.yach.kinetix.frontofhouse.data.entity.OrderItemModifierEntity
import com.yach.kinetix.frontofhouse.data.entity.OrderStatus
import com.yach.kinetix.frontofhouse.data.entity.OrderType
import com.yach.kinetix.frontofhouse.data.entity.TableStatus
import com.yach.kinetix.treasury.data.dao.CashShiftDao
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
data class OrderItemParam(
    val productId: UUID,
    val quantity: BigDecimal,
    val modifierIds: List<UUID> = emptyList(),
    val itemNotes: String? = null
)

class CreateOrderUseCase @Inject constructor(
    private val orderDao: OrderDao,
    private val tableDao: TableDao,
    private val productDao: ProductDao,
    private val modifierDao: ModifierDao,
    private val customerDao: CustomerDao,
    private val cashShiftDao: CashShiftDao,
    private val auditLogDao: AuditLogDao,
    private val transactionRunner: TransactionRunner
) {

    /**
     * Crea y confirma una nueva orden calculando el Desglose Inverso de cada platillo,
     * aplicando descuentos fijos, porcentuales o asignados por cliente CRM con cuadre
     * al centavo entre la suma de renglones y el total del encabezado, asignando folio
     * correlativo y ocupando la mesa correspondiente.
     */
    suspend operator fun invoke(
        tableId: UUID?,
        serverUserId: UUID,
        customerId: UUID? = null,
        orderType: OrderType,
        deliveryPlatform: String? = null,
        tags: String? = null,
        discountType: DiscountType = DiscountType.NONE,
        discountValue: BigDecimal = BigDecimal.ZERO,
        itemsParams: List<OrderItemParam>
    ): Result<OrderEntity> = runCatching {
        require(itemsParams.isNotEmpty()) { "La orden debe contener al menos un producto" }

        transactionRunner.runInTransaction {
            val nextOrderNumber = orderDao.getNextOrderNumber()
            val activeShift = cashShiftDao.getActiveShiftSync()
            val orderId = UUID.randomUUID()

            // Determinar tipo y valor de descuento efectivo (manual o cliente CRM)
            val effectiveDiscountType: DiscountType
            val effectiveDiscountValue: BigDecimal

            val customer = customerId?.let { customerDao.getCustomerById(it) }
            if (discountType != DiscountType.NONE) {
                effectiveDiscountType = discountType
                effectiveDiscountValue = discountValue
            } else if (customer != null && customer.defaultDiscountRate > BigDecimal.ZERO) {
                effectiveDiscountType = DiscountType.PERCENTAGE
                effectiveDiscountValue = customer.defaultDiscountRate
            } else {
                effectiveDiscountType = DiscountType.NONE
                effectiveDiscountValue = BigDecimal.ZERO
            }

            var sumOriginalPrices = BigDecimal.ZERO
            val rawItems = mutableListOf<Triple<OrderItemParam, com.yach.kinetix.backoffice.data.entity.ProductEntity, BigDecimal>>()

            // Primera pasada: Calcular importes base por producto
            for (param in itemsParams) {
                val product = productDao.getProductById(param.productId)
                    ?: throw IllegalArgumentException("Producto ${param.productId} no encontrado")

                val totalLinePrice = product.finalPriceWithTax.multiply(param.quantity)
                sumOriginalPrices = sumOriginalPrices.add(totalLinePrice)
                rawItems.add(Triple(param, product, totalLinePrice))
            }

            // Desglose global de la orden
            val orderGlobalBreakdown = TaxCalculator.calculateInverseTax(
                finalPriceWithTax = sumOriginalPrices,
                taxRate = BigDecimal("0.16"),
                discountType = effectiveDiscountType,
                discountValue = effectiveDiscountValue
            )

            // Segunda pasada: Distribuir importes en cada renglón asegurando cuadre
            val orderItemsToInsert = mutableListOf<OrderItemEntity>()
            val discountRatio = if (sumOriginalPrices > BigDecimal.ZERO && orderGlobalBreakdown.discountAmount > BigDecimal.ZERO) {
                orderGlobalBreakdown.finalTotal.divide(sumOriginalPrices, 6, java.math.RoundingMode.HALF_UP)
            } else {
                BigDecimal.ONE
            }

            for ((param, product, totalLinePrice) in rawItems) {
                val discountedLineTotal = totalLinePrice.multiply(discountRatio).setScale(2, java.math.RoundingMode.HALF_UP)
                val lineBreakdown = TaxCalculator.calculateInverseTax(discountedLineTotal, product.taxRate)

                orderItemsToInsert.add(
                    OrderItemEntity(
                        id = UUID.randomUUID(),
                        orderId = orderId,
                        productId = product.id,
                        productName = product.name,
                        quantity = param.quantity,
                        unitPriceWithTax = product.finalPriceWithTax,
                        taxRate = product.taxRate,
                        subtotalWithTax = lineBreakdown.finalTotal,
                        calculatedBase = lineBreakdown.baseAmount,
                        calculatedTax = lineBreakdown.taxAmount,
                        itemNotes = param.itemNotes,
                        kdsStatus = if (product.isKitchenItem) KdsItemStatus.PENDING else KdsItemStatus.READY,
                        isPaid = false
                    )
                )
            }

            val newOrder = OrderEntity(
                id = orderId,
                orderNumber = nextOrderNumber,
                tableId = tableId,
                serverUserId = serverUserId,
                shiftId = activeShift?.id,
                customerId = customerId,
                orderType = orderType,
                deliveryPlatform = deliveryPlatform,
                status = OrderStatus.PENDING,
                subtotalOriginal = sumOriginalPrices,
                discountType = effectiveDiscountType,
                discountRate = effectiveDiscountValue,
                discountAmount = orderGlobalBreakdown.discountAmount,
                totalAmount = orderGlobalBreakdown.finalTotal,
                baseAmount = orderGlobalBreakdown.baseAmount,
                taxAmount = orderGlobalBreakdown.taxAmount,
                tipAmount = BigDecimal.ZERO,
                tags = tags,
                isPaid = false,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )

            orderDao.insertOrder(newOrder)
            orderDao.insertOrderItems(orderItemsToInsert)

            if (tableId != null && orderType == OrderType.DINE_IN) {
                tableDao.updateTableStatus(
                    tableId = tableId,
                    status = TableStatus.OCCUPIED,
                    currentOrderId = orderId
                )
            }

            auditLogDao.insertLog(
                AuditLogEntity(
                    userId = serverUserId,
                    action = "CREATE_ORDER",
                    entityName = "Order",
                    entityId = orderId.toString(),
                    details = "Orden creada #$nextOrderNumber. Total: $$${orderGlobalBreakdown.finalTotal} (Descuento: $$${orderGlobalBreakdown.discountAmount}). Cliente: ${customer?.fullName ?: "N/A"}. Mesa: ${tableId ?: "N/A"}"
                )
            )

            newOrder
        }
    }
}
