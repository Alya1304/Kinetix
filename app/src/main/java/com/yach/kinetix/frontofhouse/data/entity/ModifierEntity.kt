package com.yach.kinetix.frontofhouse.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.util.UUID

@Entity(tableName = "modifiers")
data class ModifierEntity(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val extraPriceWithTax: BigDecimal = BigDecimal.ZERO,
    val taxRate: BigDecimal = BigDecimal("0.16"),
    val isActive: Boolean = true
)
