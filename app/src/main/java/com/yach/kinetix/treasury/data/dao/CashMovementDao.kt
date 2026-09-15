package com.yach.kinetix.treasury.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yach.kinetix.treasury.data.entity.CashMovementEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface CashMovementDao {

    @Query("SELECT * FROM cash_movements WHERE shiftId = :shiftId ORDER BY timestamp ASC")
    fun getMovementsForShift(shiftId: UUID): Flow<List<CashMovementEntity>>

    @Query("SELECT * FROM cash_movements WHERE shiftId = :shiftId ORDER BY timestamp ASC")
    suspend fun getMovementsForShiftSync(shiftId: UUID): List<CashMovementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: CashMovementEntity)
}
