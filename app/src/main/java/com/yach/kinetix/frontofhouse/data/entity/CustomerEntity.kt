package com.yach.kinetix.frontofhouse.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["phone"]),
        Index(value = ["fullName"])
    ]
)
data class CustomerEntity(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    val fullName: String,
    val taxId: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val defaultDiscountRate: BigDecimal = BigDecimal.ZERO,
    val creditLimit: BigDecimal = BigDecimal.ZERO,
    val currentDebt: BigDecimal = BigDecimal.ZERO,
    val isActive: Boolean = true,
    val createdAt: Instant = Instant.now()
)
