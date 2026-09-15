package com.yach.kinetix.frontofhouse.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.yach.kinetix.frontofhouse.data.entity.RestaurantTableEntity
import com.yach.kinetix.frontofhouse.data.entity.TableStatus
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface TableDao {

    @Query("SELECT * FROM restaurant_tables ORDER BY tableNumber ASC")
    fun getAllTables(): Flow<List<RestaurantTableEntity>>

    @Query("SELECT * FROM restaurant_tables WHERE id = :id LIMIT 1")
    suspend fun getTableById(id: UUID): RestaurantTableEntity?

    @Query("SELECT * FROM restaurant_tables WHERE status = :status ORDER BY tableNumber ASC")
    fun getTablesByStatus(status: TableStatus): Flow<List<RestaurantTableEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTable(table: RestaurantTableEntity)

    @Update
    suspend fun updateTable(table: RestaurantTableEntity)

    @Query("UPDATE restaurant_tables SET status = :status, currentOrderId = :currentOrderId WHERE id = :tableId")
    suspend fun updateTableStatus(tableId: UUID, status: TableStatus, currentOrderId: UUID?)
}
