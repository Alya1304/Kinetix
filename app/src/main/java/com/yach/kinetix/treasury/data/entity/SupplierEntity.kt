package com.yach.kinetix.treasury.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

enum class SupplierMovementType {
    CREDIT_PURCHASE, // Compra a credito (aumenta deuda con proveedor)
    PAYMENT_ABONO    // Pago o abono (disminuye deuda con proveedor)
}

@Entity(
    tableName = "suppliers",
    indices = [
        Index(value = ["name"]),
        Index(value = ["category"])
    ]
)
data class SupplierEntity(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val contactName: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val rfc: String? = null,
    val category: String? = null,
    val currentDebt: BigDecimal = BigDecimal.ZERO,
    val isActive: Boolean = true,
    val createdAt: Instant = Instant.now()
)

@Entity(
    tableName = "supplier_account_movements",
    foreignKeys = [
        ForeignKey(
            entity = SupplierEntity::class,
            parentColumns = ["id"],
            childColumns = ["supplierId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["supplierId"]),
        Index(value = ["timestamp"])
    ]
)
data class SupplierAccountMovementEntity(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    val supplierId: UUID,
    val expenseId: UUID? = null,
    val type: SupplierMovementType,
    val amount: BigDecimal,
    val notes: String? = null,
    val registeredByUserId: UUID,
    val timestamp: Instant = Instant.now()
)
