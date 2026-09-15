package com.yach.kinetix.backoffice.data.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.yach.kinetix.backoffice.data.entity.ProductEntity
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.util.UUID

@Dao
interface ProductDao {

    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE isFavorite = 1 AND isActive = 1 ORDER BY name ASC")
    fun getFavoriteProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY name ASC")
    fun getProductsPaged(): PagingSource<Int, ProductEntity>

    @Query("SELECT * FROM products WHERE categoryId = :categoryId AND isActive = 1 ORDER BY name ASC")
    fun getProductsByCategory(categoryId: UUID): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: UUID): ProductEntity?

    @Query("SELECT * FROM products WHERE sku = :sku OR barcode = :barcode LIMIT 1")
    suspend fun getProductBySkuOrBarcode(sku: String, barcode: String?): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("UPDATE products SET stockQuantity = :newStock, updatedAt = :updatedAt WHERE id = :productId")
    suspend fun updateStock(productId: UUID, newStock: BigDecimal, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE products SET stockQuantity = stockQuantity - :deductedStock, updatedAt = :updatedAt WHERE id = :productId")
    suspend fun deductStock(productId: UUID, deductedStock: BigDecimal, updatedAt: Long = System.currentTimeMillis())
}
