package com.yach.kinetix.frontofhouse.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class TableStatus {
    AVAILABLE,
    OCCUPIED,
    PAYING
}

@Entity(
    tableName = "restaurant_tables",
    foreignKeys = [
        ForeignKey(
            entity = RoomEntity::class,
            parentColumns = ["id"],
            childColumns = ["roomId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["tableNumber"], unique = true),
        Index(value = ["roomId"]),
        Index(value = ["status"])
    ]
)
data class RestaurantTableEntity(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    val tableNumber: String,
    val roomId: UUID? = null,
    val capacity: Int = 4,
    val status: TableStatus = TableStatus.AVAILABLE,
    val currentOrderId: UUID? = null,
    val tags: String? = null,
    val positionX: Float = 0f,
    val positionY: Float = 0f
)
