package com.yach.kinetix.treasury.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.yach.kinetix.backoffice.data.entity.UserEntity
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

enum class ShiftStatus {
    OPEN,
    CLOSED
}

@Entity(
    tableName = "cash_shifts",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["cashierId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = CashRegisterEntity::class,
            parentColumns = ["id"],
            childColumns = ["cashRegisterId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["cashierId"]),
        Index(value = ["cashRegisterId"]),
        Index(value = ["status"])
    ]
)
data class CashShiftEntity(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    val cashRegisterId: UUID? = null,
    val cashierId: UUID,
    val openedAt: Instant = Instant.now(),
    val closedAt: Instant? = null,
    val initialCash: BigDecimal,
    val declaredCash: BigDecimal? = null,
    val declaredCards: BigDecimal? = null,
    val expectedCash: BigDecimal? = null,
    val differenceCash: BigDecimal? = null,
    val status: ShiftStatus = ShiftStatus.OPEN,
    val notes: String? = null
)
