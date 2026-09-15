package com.yach.kinetix.backoffice.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity(
    tableName = "wastage_logs",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["reportedByUserId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["productId"]),
        Index(value = ["reportedByUserId"])
    ]
)
data class WastageEntity(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    val productId: UUID,
    val quantity: BigDecimal,
    val reason: String,
    val reportedByUserId: UUID,
    val registeredAt: Instant = Instant.now()
)
