package com.yach.kinetix.treasury.domain

import com.yach.kinetix.backoffice.data.dao.AuditLogDao
import com.yach.kinetix.backoffice.data.entity.AuditLogEntity
import com.yach.kinetix.treasury.data.dao.CashMovementDao
import com.yach.kinetix.treasury.data.dao.CashShiftDao
import com.yach.kinetix.treasury.data.entity.CashMovementEntity
import com.yach.kinetix.treasury.data.entity.CashMovementType
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class RegisterCashMovementUseCase @Inject constructor(
    private val cashShiftDao: CashShiftDao,
    private val cashMovementDao: CashMovementDao,
    private val auditLogDao: AuditLogDao
) {

    /**
     * Registra un movimiento de efectivo (ingreso de cambio o retiro para gastos)
     * asociado al turno de caja activo, con justificación y usuario autorizador.
     */
    suspend operator fun invoke(
        type: CashMovementType,
        amount: BigDecimal,
        reason: String,
        authorizedByUserId: UUID
    ): Result<CashMovementEntity> = runCatching {
        require(amount > BigDecimal.ZERO) { "El monto del movimiento debe ser mayor a cero" }
        require(reason.isNotBlank()) { "Se requiere especificar un motivo justificable para el movimiento" }

        val activeShift = cashShiftDao.getActiveShiftSync()
            ?: throw IllegalStateException("No existe un turno de caja abierto para registrar movimientos")

        val movement = CashMovementEntity(
            id = UUID.randomUUID(),
            shiftId = activeShift.id,
            type = type,
            amount = amount,
            reason = reason,
            authorizedByUserId = authorizedByUserId,
            timestamp = Instant.now()
        )

        cashMovementDao.insertMovement(movement)

        auditLogDao.insertLog(
            AuditLogEntity(
                userId = authorizedByUserId,
                action = "CASH_MOVEMENT_${type.name}",
                entityName = "CashMovement",
                entityId = movement.id.toString(),
                details = "Movimiento $type por $$amount. Motivo: $reason. Turno: ${activeShift.id}"
            )
        )

        movement
    }
}
