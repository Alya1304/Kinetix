package com.yach.kinetix.treasury.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.yach.kinetix.treasury.data.entity.SupplierAccountMovementEntity
import com.yach.kinetix.treasury.data.entity.SupplierEntity
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.util.UUID

@Dao
interface SupplierDao {

    @Query("SELECT * FROM suppliers WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveSuppliers(): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM suppliers WHERE id = :id LIMIT 1")
    suspend fun getSupplierById(id: UUID): SupplierEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: SupplierEntity)

    @Update
    suspend fun updateSupplier(supplier: SupplierEntity)

    @Query("UPDATE suppliers SET currentDebt = currentDebt + :amountDelta WHERE id = :supplierId")
    suspend fun adjustDebt(supplierId: UUID, amountDelta: BigDecimal)

    @Query("SELECT * FROM supplier_account_movements WHERE supplierId = :supplierId ORDER BY timestamp DESC")
    fun getMovementsForSupplier(supplierId: UUID): Flow<List<SupplierAccountMovementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: SupplierAccountMovementEntity)
}
