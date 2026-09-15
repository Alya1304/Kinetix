package com.yach.kinetix.backoffice.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity(
    tableName = "ingredients",
    indices = [
        Index(value = ["name"])
    ]
)
data class IngredientEntity(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val unitOfMeasure: String, // ej. "kg", "lt", "gr", "ml", "pza"
    val stockQuantity: BigDecimal = BigDecimal.ZERO,
    val costPerUnit: BigDecimal = BigDecimal.ZERO,
    val minStockAlert: BigDecimal = BigDecimal.ZERO,
    val isActive: Boolean = true,
    val updatedAt: Instant = Instant.now()
)

@Entity(
    tableName = "recipe_items",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = IngredientEntity::class,
            parentColumns = ["id"],
            childColumns = ["ingredientId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["productId"]),
        Index(value = ["ingredientId"])
    ]
)
data class RecipeItemEntity(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    val productId: UUID,
    val ingredientId: UUID,
    val requiredQuantity: BigDecimal
)
