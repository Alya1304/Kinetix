package com.yach.kinetix.backoffice.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.yach.kinetix.backoffice.data.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories WHERE isActive = 1 AND parentCategoryId IS NULL ORDER BY sortOrder ASC, name ASC")
    fun getRootCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE parentCategoryId = :parentId AND isActive = 1 ORDER BY sortOrder ASC, name ASC")
    fun getSubcategories(parentId: UUID): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE isActive = 1 ORDER BY sortOrder ASC, name ASC")
    fun getAllActiveCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)
}
