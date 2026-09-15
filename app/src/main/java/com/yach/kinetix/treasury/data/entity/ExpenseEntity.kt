package com.yach.kinetix.treasury.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.yach.kinetix.frontofhouse.data.entity.PaymentMethod
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

enum class ExpenseStatus {
    PENDING,
    PAID
}

@Entity(tableName = "expense_categories")
data class ExpenseCategoryEntity(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val isActive: Boolean = true
)

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = ExpenseCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = SupplierEntity::class,
            parentColumns = ["id"],
            childColumns = ["supplierId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = CashShiftEntity::class,
            parentColumns = ["id"],
            childColumns = ["shiftId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["supplierId"]),
        Index(value = ["shiftId"]),
        Index(value = ["dueDate"]),
        Index(value = ["status"])
    ]
)
data class ExpenseEntity(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    val shiftId: UUID? = null,
    val categoryId: UUID,
    val supplierId: UUID? = null,
    val amount: BigDecimal,
    val dueDate: Instant? = null,
    val paidDate: Instant? = null,
    val paymentMethod: PaymentMethod? = null,
    val status: ExpenseStatus = ExpenseStatus.PAID,
    val notes: String? = null,
    val authorizedByUserId: UUID,
    val createdAt: Instant = Instant.now()
)
