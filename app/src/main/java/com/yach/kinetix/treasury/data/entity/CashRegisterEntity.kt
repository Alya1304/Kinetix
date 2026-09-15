package com.yach.kinetix.treasury.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "cash_registers")
data class CashRegisterEntity(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val isActive: Boolean = true
)
