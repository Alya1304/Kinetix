package com.yach.kinetix.backoffice.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yach.kinetix.backoffice.data.entity.WastageEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface WastageDao {

    @Query("SELECT * FROM wastage_logs ORDER BY registeredAt DESC")
    fun getAllWastageLogs(): Flow<List<WastageEntity>>

    @Query("SELECT * FROM wastage_logs WHERE productId = :productId ORDER BY registeredAt DESC")
    fun getWastageForProduct(productId: UUID): Flow<List<WastageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWastage(wastage: WastageEntity)
}
