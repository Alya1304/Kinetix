package com.yach.kinetix.frontofhouse.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.util.UUID

@Entity(
    tableName = "order_item_modifiers",
    foreignKeys = [
        ForeignKey(
            entity = OrderItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderItemId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ModifierEntity::class,
            parentColumns = ["id"],
            childColumns = ["modifierId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["orderItemId"]),
        Index(value = ["modifierId"])
    ]
)
data class OrderItemModifierEntity(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    val orderItemId: UUID,
    val modifierId: UUID,
    val modifierName: String,
    val extraPriceWithTax: BigDecimal = BigDecimal.ZERO
)
