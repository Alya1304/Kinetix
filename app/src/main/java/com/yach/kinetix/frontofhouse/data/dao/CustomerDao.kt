package com.yach.kinetix.frontofhouse.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.yach.kinetix.frontofhouse.data.entity.CustomerAccountMovementEntity
import com.yach.kinetix.frontofhouse.data.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.util.UUID

@Dao
interface CustomerDao {

    @Query("SELECT * FROM customers WHERE isActive = 1 ORDER BY fullName ASC")
    fun getAllActiveCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE fullName LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' ORDER BY fullName ASC")
    fun searchCustomers(query: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: UUID): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<CustomerEntity>)

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("UPDATE customers SET currentDebt = currentDebt + :amountDelta WHERE id = :customerId")
    suspend fun adjustDebt(customerId: UUID, amountDelta: BigDecimal)

    @Query("SELECT * FROM customer_account_movements WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getMovementsForCustomer(customerId: UUID): Flow<List<CustomerAccountMovementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: CustomerAccountMovementEntity)
}
