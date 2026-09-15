package com.yach.kinetix.frontofhouse.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.yach.kinetix.frontofhouse.data.entity.KdsItemStatus
import com.yach.kinetix.frontofhouse.data.entity.OrderEntity
import com.yach.kinetix.frontofhouse.data.entity.OrderItemEntity
import com.yach.kinetix.frontofhouse.data.entity.OrderItemModifierEntity
import com.yach.kinetix.frontofhouse.data.entity.OrderStatus
import com.yach.kinetix.frontofhouse.data.model.OrderWithDetails
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface OrderDao {

    @Transaction
    @Query("SELECT * FROM orders WHERE id = :orderId LIMIT 1")
    fun getOrderWithDetails(orderId: UUID): Flow<OrderWithDetails?>

    @Transaction
    @Query("SELECT * FROM orders WHERE id = :orderId LIMIT 1")
    suspend fun getOrderWithDetailsSync(orderId: UUID): OrderWithDetails?

    @Transaction
    @Query("SELECT * FROM orders WHERE status IN ('PENDING', 'IN_PREPARATION') ORDER BY createdAt ASC")
    fun getActiveKitchenOrders(): Flow<List<OrderWithDetails>>

    @Transaction
    @Query("SELECT * FROM orders WHERE tableId = :tableId AND isPaid = 0 LIMIT 1")
    fun getActiveOrderForTable(tableId: UUID): Flow<OrderWithDetails?>

    @Query("SELECT * FROM orders WHERE shiftId = :shiftId ORDER BY createdAt DESC")
    fun getOrdersForShift(shiftId: UUID): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE shiftId = :shiftId AND isPaid = 1")
    suspend fun getPaidOrdersForShiftSync(shiftId: UUID): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE createdAt BETWEEN :startDate AND :endDate ORDER BY createdAt DESC")
    fun getOrdersBetweenDates(startDate: java.time.Instant, endDate: java.time.Instant): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE createdAt BETWEEN :startDate AND :endDate AND isPaid = 1 ORDER BY createdAt DESC")
    suspend fun getPaidOrdersBetweenDatesSync(startDate: java.time.Instant, endDate: java.time.Instant): List<OrderEntity>

    @Query("SELECT COALESCE(MAX(orderNumber), 0) + 1 FROM orders")
    suspend fun getNextOrderNumber(): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItemModifiers(modifiers: List<OrderItemModifierEntity>)

    @Query("UPDATE orders SET status = :status, updatedAt = :updatedAt WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: UUID, status: OrderStatus, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE order_items SET kdsStatus = :status WHERE id = :orderItemId")
    suspend fun updateOrderItemKdsStatus(orderItemId: UUID, status: KdsItemStatus)

    @Query("UPDATE order_items SET isPaid = :isPaid, splitGroupId = :splitGroupId WHERE id = :orderItemId")
    suspend fun updateOrderItemPaymentSplit(orderItemId: UUID, isPaid: Boolean, splitGroupId: UUID?)
}
