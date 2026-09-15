package com.yach.kinetix.backoffice.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["sku"], unique = true),
        Index(value = ["isFavorite"])
    ]
)
data class ProductEntity(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    val categoryId: UUID,
    val name: String,
    val sku: String,
    val barcode: String? = null,
    val finalPriceWithTax: BigDecimal,
    val taxRate: BigDecimal = BigDecimal("0.16"),
    val stockQuantity: BigDecimal = BigDecimal.ZERO,
    val isKitchenItem: Boolean = true,
    val isFavorite: Boolean = false,
    val allowOutOfStockSale: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
