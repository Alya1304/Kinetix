package com.yach.kinetix.backoffice.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

@Entity(
    tableName = "audit_logs",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["timestamp"])
    ]
)
data class AuditLogEntity(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    val userId: UUID?,
    val action: String,
    val entityName: String,
    val entityId: String?,
    val details: String,
    val timestamp: Instant = Instant.now()
)
