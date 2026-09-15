package com.yach.kinetix.frontofhouse.domain

import com.yach.kinetix.backoffice.data.dao.AuditLogDao
import com.yach.kinetix.backoffice.data.entity.AuditLogEntity
import com.yach.kinetix.core.database.TransactionRunner
import com.yach.kinetix.frontofhouse.data.dao.CustomerDao
import com.yach.kinetix.frontofhouse.data.entity.CustomerAccountMovementEntity
import com.yach.kinetix.frontofhouse.data.entity.CustomerMovementType
import com.yach.kinetix.treasury.data.dao.CashMovementDao
import com.yach.kinetix.treasury.data.dao.CashShiftDao
import com.yach.kinetix.treasury.data.entity.CashMovementEntity
import com.yach.kinetix.treasury.data.entity.CashMovementType
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class RegisterCustomerPaymentUseCase @Inject constructor(
    private val customerDao: CustomerDao,
    private val cashShiftDao: CashShiftDao,
    private val cashMovementDao: CashMovementDao,
    private val auditLogDao: AuditLogDao,
    private val transactionRunner: TransactionRunner
) {

    /**
     * Registra un abono a la cuenta corriente del cliente reduciendo su deuda acumulada.
     * Si el abono se realiza en efectivo, ingresa el dinero al turno de caja activo.
     */
    suspend operator fun invoke(
        customerId: UUID,
        amount: BigDecimal,
        isCashPayment: Boolean,
        notes: String?,
        cashierUserId: UUID
    ): Result<Unit> = runCatching {
        require(amount > BigDecimal.ZERO) { "El monto del abono debe ser mayor a cero" }

        transactionRunner.runInTransaction {
            val customer = customerDao.getCustomerById(customerId)
                ?: throw IllegalArgumentException("Cliente no encontrado")

            // 1. Registrar abono en cuenta corriente
            val movement = CustomerAccountMovementEntity(
                id = UUID.randomUUID(),
                customerId = customerId,
                type = CustomerMovementType.PAYMENT_ABONO,
                amount = amount,
                notes = notes,
                registeredByUserId = cashierUserId,
                timestamp = Instant.now()
            )
            customerDao.insertMovement(movement)

            // Reducir la deuda del cliente (restando amount)
            customerDao.adjustDebt(customerId, amount.negate())

            // 2. Si es pago en efectivo, sumar al turno de caja activo
            if (isCashPayment) {
                val activeShift = cashShiftDao.getActiveShiftSync()
                if (activeShift != null) {
                    cashMovementDao.insertMovement(
                        CashMovementEntity(
                            id = UUID.randomUUID(),
                            shiftId = activeShift.id,
                            type = CashMovementType.CASH_IN,
                            amount = amount,
                            reason = "Abono a cuenta corriente de ${customer.fullName}",
                            authorizedByUserId = cashierUserId,
                            timestamp = Instant.now()
                        )
                    )
                }
            }

            // 3. Bitácora forense
            auditLogDao.insertLog(
                AuditLogEntity(
                    userId = cashierUserId,
                    action = "CUSTOMER_CREDIT_PAYMENT",
                    entityName = "Customer",
                    entityId = customerId.toString(),
                    details = "Abono de $$amount a la cuenta corriente de ${customer.fullName}. Deuda restante aproximada: $${customer.currentDebt.subtract(amount)}"
                )
            )
        }
    }
}
