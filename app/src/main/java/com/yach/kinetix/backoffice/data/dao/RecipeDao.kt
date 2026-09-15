package com.yach.kinetix.backoffice.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yach.kinetix.backoffice.data.entity.RecipeItemEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface RecipeDao {

    @Query("SELECT * FROM recipe_items WHERE productId = :productId")
    fun getRecipeForProduct(productId: UUID): Flow<List<RecipeItemEntity>>

    @Query("SELECT * FROM recipe_items WHERE productId = :productId")
    suspend fun getRecipeForProductSync(productId: UUID): List<RecipeItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipeItem(item: RecipeItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipeItems(items: List<RecipeItemEntity>)

    @Query("DELETE FROM recipe_items WHERE productId = :productId")
    suspend fun deleteRecipeForProduct(productId: UUID)
}
