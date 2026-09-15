package com.yach.kinetix.frontofhouse.data.model

import androidx.room.Embedded
import androidx.room.Relation
import com.yach.kinetix.frontofhouse.data.entity.OrderEntity
import com.yach.kinetix.frontofhouse.data.entity.OrderItemEntity
import com.yach.kinetix.frontofhouse.data.entity.OrderItemModifierEntity

data class OrderItemWithModifiers(
    @Embedded
    val item: OrderItemEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "orderItemId"
    )
    val modifiers: List<OrderItemModifierEntity> = emptyList()
)

data class OrderWithDetails(
    @Embedded
    val order: OrderEntity,

    @Relation(
        entity = OrderItemEntity::class,
        parentColumn = "id",
        entityColumn = "orderId"
    )
    val items: List<OrderItemWithModifiers> = emptyList()
)
