package com.yach.kinetix.backoffice.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentCategoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["parentCategoryId"])
    ]
)
data class CategoryEntity(
    @PrimaryKey
    val id: UUID = UUID.randomUUID(),
    val parentCategoryId: UUID? = null,
    val name: String,
    val sortOrder: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Instant = Instant.now()
)
