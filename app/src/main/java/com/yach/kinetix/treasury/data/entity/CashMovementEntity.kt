package com.yach.kinetix.treasury.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.yach.kinetix.backoffice.data.entity.UserEntity
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

enum class CashMovementType {
    CASH_IN,
    CASH_OUT
}

@Entity(
    tableName = "cash_movements",
    foreignKeys = [
        ForeignKey(
            entity = CashShiftEntity::class,
            parentColumns = ["id"],
            childColumns = ["shiftId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["authorizedByUserId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["shiftId"]),
        Index(value = ["authorizedByUserId"])
    ]
)
data class CashMovementEntity(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    val shiftId: UUID,
    val type: CashMovementType,
    val amount: BigDecimal,
    val reason: String,
    val authorizedByUserId: UUID,
    val timestamp: Instant = Instant.now()
)
