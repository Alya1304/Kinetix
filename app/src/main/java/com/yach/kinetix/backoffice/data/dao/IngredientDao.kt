package com.yach.kinetix.backoffice.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.yach.kinetix.backoffice.data.entity.IngredientEntity
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.util.UUID

@Dao
interface IngredientDao {

    @Query("SELECT * FROM ingredients WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveIngredients(): Flow<List<IngredientEntity>>

    @Query("SELECT * FROM ingredients WHERE stockQuantity <= minStockAlert AND isActive = 1 ORDER BY name ASC")
    fun getLowStockIngredients(): Flow<List<IngredientEntity>>

    @Query("SELECT * FROM ingredients WHERE id = :id LIMIT 1")
    suspend fun getIngredientById(id: UUID): IngredientEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredient(ingredient: IngredientEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredients(ingredients: List<IngredientEntity>)

    @Update
    suspend fun updateIngredient(ingredient: IngredientEntity)

    @Query("UPDATE ingredients SET stockQuantity = stockQuantity - :deductedStock, updatedAt = :updatedAt WHERE id = :ingredientId")
    suspend fun deductStock(ingredientId: UUID, deductedStock: BigDecimal, updatedAt: Long = System.currentTimeMillis())
}
