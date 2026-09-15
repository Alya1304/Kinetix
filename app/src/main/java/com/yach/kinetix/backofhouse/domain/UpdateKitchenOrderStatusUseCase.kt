package com.yach.kinetix.backofhouse.domain

import com.yach.kinetix.backoffice.data.dao.AuditLogDao
import com.yach.kinetix.backoffice.data.entity.AuditLogEntity
import com.yach.kinetix.frontofhouse.data.dao.OrderDao
import com.yach.kinetix.frontofhouse.data.entity.KdsItemStatus
import com.yach.kinetix.frontofhouse.data.entity.OrderStatus
import java.util.UUID
import javax.inject.Inject

class UpdateKitchenOrderStatusUseCase @Inject constructor(
    private val orderDao: OrderDao,
    private val auditLogDao: AuditLogDao
) {

    /**
     * Actualiza el estado de preparación de un pedido en la pantalla KDS de cocina
     * (de PENDING a IN_PREPARATION o a READY).
     */
    suspend fun updateOrderStatus(
        orderId: UUID,
        newStatus: OrderStatus,
        cookUserId: UUID
    ): Result<Unit> = runCatching {
        orderDao.updateOrderStatus(orderId, newStatus)

        auditLogDao.insertLog(
            AuditLogEntity(
                userId = cookUserId,
                action = "KDS_UPDATE_ORDER_STATUS",
                entityName = "Order",
                entityId = orderId.toString(),
                details = "Estado de orden en cocina actualizado a: $newStatus"
            )
        )
    }

    /**
     * Actualiza el estado de preparación de una partida/platillo individual en cocina.
     */
    suspend fun updateItemStatus(
        orderItemId: UUID,
        newStatus: KdsItemStatus,
        cookUserId: UUID
    ): Result<Unit> = runCatching {
        orderDao.updateOrderItemKdsStatus(orderItemId, newStatus)

        auditLogDao.insertLog(
            AuditLogEntity(
                userId = cookUserId,
                action = "KDS_UPDATE_ITEM_STATUS",
                entityName = "OrderItem",
                entityId = orderItemId.toString(),
                details = "Platillo individual en cocina actualizado a: $newStatus"
            )
        )
    }
}
