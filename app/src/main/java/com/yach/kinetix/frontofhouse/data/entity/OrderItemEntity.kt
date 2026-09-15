package com.yach.kinetix.frontofhouse.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.yach.kinetix.backoffice.data.entity.ProductEntity
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

enum class KdsItemStatus {
    PENDING,
    COOKING,
    READY
}

@Entity(
    tableName = "order_items",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["orderId"]),
        Index(value = ["productId"]),
        Index(value = ["kdsStatus"])
    ]
)
data class OrderItemEntity(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    val orderId: UUID,
    val productId: UUID,
    val productName: String,
    val quantity: BigDecimal,
    val unitPriceWithTax: BigDecimal,
    val taxRate: BigDecimal = BigDecimal("0.16"),
    val subtotalWithTax: BigDecimal,
    val calculatedBase: BigDecimal,
    val calculatedTax: BigDecimal,
    val itemNotes: String? = null,
    val kdsStatus: KdsItemStatus = KdsItemStatus.PENDING,
    val isPaid: Boolean = false,
    val splitGroupId: UUID? = null,
    val createdAt: Instant = Instant.now()
)
