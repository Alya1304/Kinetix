package com.yach.kinetix.backoffice.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yach.kinetix.backoffice.data.entity.PriceListEntity
import com.yach.kinetix.backoffice.data.entity.ProductPriceEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface PriceListDao {

    @Query("SELECT * FROM price_lists ORDER BY isDefault DESC, name ASC")
    fun getAllPriceLists(): Flow<List<PriceListEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceList(priceList: PriceListEntity)

    @Query("SELECT * FROM product_prices WHERE priceListId = :priceListId AND productId = :productId LIMIT 1")
    suspend fun getProductPrice(priceListId: UUID, productId: UUID): ProductPriceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProductPrice(productPrice: ProductPriceEntity)
}
