package com.yach.kinetix.backoffice.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yach.kinetix.backoffice.data.entity.StockAuditEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface StockAuditDao {

    @Query("SELECT * FROM stock_audits ORDER BY auditedAt DESC")
    fun getAllAudits(): Flow<List<StockAuditEntity>>

    @Query("SELECT * FROM stock_audits WHERE productId = :productId ORDER BY auditedAt DESC")
    fun getAuditsForProduct(productId: UUID): Flow<List<StockAuditEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudit(audit: StockAuditEntity)
}
