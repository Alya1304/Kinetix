package com.yach.kinetix.treasury.domain

import com.yach.kinetix.backoffice.data.dao.AuditLogDao
import com.yach.kinetix.backoffice.data.entity.AuditLogEntity
import com.yach.kinetix.treasury.data.dao.CashShiftDao
import com.yach.kinetix.treasury.data.entity.CashShiftEntity
import com.yach.kinetix.treasury.data.entity.ShiftStatus
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class OpenShiftUseCase @Inject constructor(
    private val cashShiftDao: CashShiftDao,
    private val auditLogDao: AuditLogDao
) {

    /**
     * Inicia un nuevo turno de caja declarando el fondo base inicial.
     * Si se especifica una caja registradora, valida la no concurrencia para esa caja en particular,
     * permitiendo que múltiples cajas operen de forma independiente.
     */
    suspend operator fun invoke(
        cashierUserId: UUID,
        initialCash: BigDecimal,
        cashRegisterId: UUID? = null
    ): Result<CashShiftEntity> = runCatching {
        require(initialCash >= BigDecimal.ZERO) { "El fondo inicial no puede ser negativo" }

        val activeShift = if (cashRegisterId != null) {
            cashShiftDao.getActiveShiftForRegister(cashRegisterId)
        } else {
            cashShiftDao.getActiveShiftSync()
        }

        if (activeShift != null) {
            val registerText = if (cashRegisterId != null) "para esta caja registradora" else "en el sistema"
            throw IllegalStateException("Ya existe un turno de caja abierto $registerText (Turno ID: ${activeShift.id}). Debe cerrarse antes de abrir uno nuevo.")
        }

        val newShift = CashShiftEntity(
            id = UUID.randomUUID(),
            cashRegisterId = cashRegisterId,
            cashierId = cashierUserId,
            openedAt = Instant.now(),
            initialCash = initialCash,
            status = ShiftStatus.OPEN
        )

        cashShiftDao.insertShift(newShift)

        auditLogDao.insertLog(
            AuditLogEntity(
                userId = cashierUserId,
                action = "OPEN_SHIFT",
                entityName = "CashShift",
                entityId = newShift.id.toString(),
                details = "Apertura de turno exitosa con fondo inicial de $$initialCash. Caja: ${cashRegisterId ?: "General"}"
            )
        )

        newShift
    }
}
