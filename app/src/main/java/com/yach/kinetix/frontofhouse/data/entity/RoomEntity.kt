package com.yach.kinetix.frontofhouse.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "rooms")
data class RoomEntity(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val sortOrder: Int = 0
)
