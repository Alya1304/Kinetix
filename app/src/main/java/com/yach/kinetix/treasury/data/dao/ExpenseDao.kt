package com.yach.kinetix.treasury.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.yach.kinetix.treasury.data.entity.ExpenseCategoryEntity
import com.yach.kinetix.treasury.data.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses ORDER BY createdAt DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE status = 'PENDING' ORDER BY dueDate ASC")
    fun getPendingExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE createdAt BETWEEN :startDate AND :endDate ORDER BY createdAt DESC")
    fun getExpensesBetweenDates(startDate: java.time.Instant, endDate: java.time.Instant): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE shiftId = :shiftId AND paymentMethod = 'CASH'")
    suspend fun getCashExpensesForShift(shiftId: UUID): List<ExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Query("SELECT * FROM expense_categories WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveCategories(): Flow<List<ExpenseCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: ExpenseCategoryEntity)
}
