package com.yach.kinetix.frontofhouse.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

enum class CustomerMovementType {
    CREDIT_PURCHASE, // Consumo a credito (suma a la deuda)
    PAYMENT_ABONO    // Abono a deuda (resta a la deuda)
}

@Entity(
    tableName = "customer_account_movements",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["timestamp"])
    ]
)
data class CustomerAccountMovementEntity(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    val customerId: UUID,
    val orderId: UUID? = null,
    val type: CustomerMovementType,
    val amount: BigDecimal,
    val notes: String? = null,
    val registeredByUserId: UUID,
    val timestamp: Instant = Instant.now()
)
