package com.yach.kinetix.frontofhouse.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.yach.kinetix.frontofhouse.data.entity.RestaurantTableEntity
import com.yach.kinetix.frontofhouse.data.entity.RoomEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface RoomDao {

    @Query("SELECT * FROM rooms ORDER BY sortOrder ASC, name ASC")
    fun getAllRooms(): Flow<List<RoomEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoom(room: RoomEntity)

    @Update
    suspend fun updateRoom(room: RoomEntity)

    @Query("SELECT * FROM restaurant_tables WHERE roomId = :roomId ORDER BY tableNumber ASC")
    fun getTablesByRoom(roomId: UUID): Flow<List<RestaurantTableEntity>>
}
