package com.yach.kinetix.treasury.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.yach.kinetix.treasury.data.entity.CashShiftEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface CashShiftDao {

    @Query("SELECT * FROM cash_shifts WHERE status = 'OPEN' LIMIT 1")
    fun getActiveShift(): Flow<CashShiftEntity?>

    @Query("SELECT * FROM cash_shifts WHERE status = 'OPEN' LIMIT 1")
    suspend fun getActiveShiftSync(): CashShiftEntity?

    @Query("SELECT * FROM cash_shifts WHERE cashRegisterId = :registerId AND status = 'OPEN' LIMIT 1")
    suspend fun getActiveShiftForRegister(registerId: UUID): CashShiftEntity?

    @Query("SELECT * FROM cash_shifts WHERE status = 'OPEN'")
    fun getAllActiveShifts(): Flow<List<CashShiftEntity>>

    @Query("SELECT * FROM cash_shifts WHERE id = :id LIMIT 1")
    suspend fun getShiftById(id: UUID): CashShiftEntity?

    @Query("SELECT * FROM cash_shifts ORDER BY openedAt DESC")
    fun getAllShifts(): Flow<List<CashShiftEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: CashShiftEntity)

    @Update
    suspend fun updateShift(shift: CashShiftEntity)
}
