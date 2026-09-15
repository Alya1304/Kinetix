package com.yach.kinetix.frontofhouse.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.yach.kinetix.frontofhouse.data.entity.ModifierEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface ModifierDao {

    @Query("SELECT * FROM modifiers WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveModifiers(): Flow<List<ModifierEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModifier(modifier: ModifierEntity)

    @Update
    suspend fun updateModifier(modifier: ModifierEntity)
}
