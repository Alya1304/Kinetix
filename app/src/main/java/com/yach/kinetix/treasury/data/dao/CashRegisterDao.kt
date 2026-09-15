package com.yach.kinetix.treasury.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yach.kinetix.treasury.data.entity.CashRegisterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CashRegisterDao {

    @Query("SELECT * FROM cash_registers WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveRegisters(): Flow<List<CashRegisterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegister(register: CashRegisterEntity)
}
