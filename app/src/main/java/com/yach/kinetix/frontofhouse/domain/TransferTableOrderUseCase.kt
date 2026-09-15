package com.yach.kinetix.frontofhouse.domain

import com.yach.kinetix.backoffice.data.dao.AuditLogDao
import com.yach.kinetix.backoffice.data.entity.AuditLogEntity
import com.yach.kinetix.core.database.TransactionRunner
import com.yach.kinetix.frontofhouse.data.dao.OrderDao
import com.yach.kinetix.frontofhouse.data.dao.TableDao
import com.yach.kinetix.frontofhouse.data.entity.TableStatus
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class TransferTableOrderUseCase @Inject constructor(
    private val tableDao: TableDao,
    private val orderDao: OrderDao,
    private val auditLogDao: AuditLogDao,
    private val transactionRunner: TransactionRunner
) {

    /**
     * Traslada una orden activa y sus comensales de una mesa de origen a una mesa de destino libre.
     */
    suspend operator fun invoke(
        sourceTableId: UUID,
        targetTableId: UUID,
        authorizedByUserId: UUID
    ): Result<Unit> = runCatching {
        require(sourceTableId != targetTableId) { "La mesa de origen y destino deben ser distintas" }

        transactionRunner.runInTransaction {
            val sourceTable = tableDao.getTableById(sourceTableId)
                ?: throw IllegalArgumentException("Mesa de origen no encontrada")

            val targetTable = tableDao.getTableById(targetTableId)
                ?: throw IllegalArgumentException("Mesa de destino no encontrada")

            val activeOrderId = sourceTable.currentOrderId
                ?: throw IllegalStateException("La mesa #${sourceTable.tableNumber} no cuenta con una orden activa para trasladar")

            if (targetTable.status != TableStatus.AVAILABLE) {
                throw IllegalStateException("La mesa de destino #${targetTable.tableNumber} se encuentra ocupada")
            }

            val orderDetails = orderDao.getOrderWithDetailsSync(activeOrderId)
                ?: throw IllegalStateException("Orden activa no encontrada")

            // 1. Reasignar orden a la nueva mesa
            val updatedOrder = orderDetails.order.copy(
                tableId = targetTableId,
                updatedAt = Instant.now()
            )
            orderDao.updateOrder(updatedOrder)

            // 2. Liberar mesa de origen
            tableDao.updateTableStatus(
                tableId = sourceTableId,
                status = TableStatus.AVAILABLE,
                currentOrderId = null
            )

            // 3. Ocupar mesa de destino
            tableDao.updateTableStatus(
                tableId = targetTableId,
                status = TableStatus.OCCUPIED,
                currentOrderId = activeOrderId
            )

            // 4. Registro de auditoria
            auditLogDao.insertLog(
                AuditLogEntity(
                    userId = authorizedByUserId,
                    action = "TRANSFER_TABLE_ORDER",
                    entityName = "Table",
                    entityId = targetTableId.toString(),
                    details = "Traslado de orden #${orderDetails.order.orderNumber} desde Mesa #${sourceTable.tableNumber} hacia Mesa #${targetTable.tableNumber}"
                )
            )
        }
    }
}
