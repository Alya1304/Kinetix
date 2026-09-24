package com.yach.kinetix.frontofhouse.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.yach.kinetix.backoffice.data.entity.UserEntity
import com.yach.kinetix.core.finance.DiscountType
import com.yach.kinetix.treasury.data.entity.CashShiftEntity
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

enum class OrderType {
    DINE_IN,
    TAKEAWAY,
    DELIVERY_APP
}

enum class OrderStatus {
    PENDING,
    IN_PREPARATION,
    READY,
    DELIVERED,
    CANCELLED
}

enum class PaymentMethod {
    CASH,
    CARD,
    MIXED,
    DELIVERY_PLATFORM,
    CUSTOMER_CREDIT, // Pago a cuenta corriente (Fiar)
    TRANSFER
}

@Entity(
    tableName = "orders",
    foreignKeys = [
        ForeignKey(
            entity = RestaurantTableEntity::class,
            parentColumns = ["id"],
            childColumns = ["tableId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["serverUserId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = CashShiftEntity::class,
            parentColumns = ["id"],
            childColumns = ["shiftId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["tableId"]),
        Index(value = ["serverUserId"]),
        Index(value = ["shiftId"]),
        Index(value = ["customerId"]),
        Index(value = ["status"]),
        Index(value = ["createdAt"])
    ]
)
data class OrderEntity(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    val orderNumber: Long,
    val tableId: UUID? = null,
    val serverUserId: UUID,
    val shiftId: UUID? = null,
    val customerId: UUID? = null,
    val orderType: OrderType = OrderType.DINE_IN,
    val deliveryPlatform: String? = null,
    val status: OrderStatus = OrderStatus.PENDING,
    val subtotalOriginal: BigDecimal = BigDecimal.ZERO,
    val discountType: DiscountType = DiscountType.NONE,
    val discountRate: BigDecimal = BigDecimal.ZERO,
    val discountAmount: BigDecimal = BigDecimal.ZERO,
    val totalAmount: BigDecimal = BigDecimal.ZERO,
    val baseAmount: BigDecimal = BigDecimal.ZERO,
    val taxAmount: BigDecimal = BigDecimal.ZERO,
    val tipAmount: BigDecimal = BigDecimal.ZERO,
    val paymentMethod: PaymentMethod? = null,
    val tags: String? = null,
    val isPaid: Boolean = false,
    val paidAt: Instant? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
