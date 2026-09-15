package com.yach.kinetix.backoffice.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.util.UUID

@Entity(tableName = "price_lists")
data class PriceListEntity(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    val name: String, // ej. "Salón", "Barra", "Delivery", "Nocturno"
    val isDefault: Boolean = false
)

@Entity(
    tableName = "product_prices",
    foreignKeys = [
        ForeignKey(
            entity = PriceListEntity::class,
            parentColumns = ["id"],
            childColumns = ["priceListId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["priceListId", "productId"], unique = true),
        Index(value = ["productId"])
    ]
)
data class ProductPriceEntity(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    val priceListId: UUID,
    val productId: UUID,
    val priceWithTax: BigDecimal
)

@Entity(
    tableName = "product_combo_items",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentComboProductId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["childProductId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["parentComboProductId"]),
        Index(value = ["childProductId"])
    ]
)
data class ProductComboItemEntity(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    val parentComboProductId: UUID,
    val childProductId: UUID,
    val quantity: BigDecimal = BigDecimal.ONE
)
