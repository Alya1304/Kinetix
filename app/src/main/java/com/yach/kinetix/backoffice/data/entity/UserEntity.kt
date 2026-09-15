package com.yach.kinetix.backoffice.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

enum class UserRole {
    ADMIN,
    CASHIER,
    WAITER,
    COOK
}

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    val username: String,
    val fullName: String,
    val role: UserRole,
    val pinHash: String,
    val isActive: Boolean = true,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
