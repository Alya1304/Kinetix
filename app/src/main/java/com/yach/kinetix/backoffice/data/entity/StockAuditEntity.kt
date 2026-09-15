package com.yach.kinetix.backoffice.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity(
    tableName = "stock_audits",
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
            childColumns = ["auditedByUserId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["productId"]),
        Index(value = ["auditedByUserId"])
    ]
)
data class StockAuditEntity(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    val productId: UUID,
    val expectedStock: BigDecimal,
    val countedStock: BigDecimal,
    val difference: BigDecimal,
    val auditedByUserId: UUID,
    val notes: String? = null,
    val auditedAt: Instant = Instant.now()
)
