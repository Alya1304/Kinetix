package com.yach.kinetix.treasury.domain

import com.yach.kinetix.backoffice.data.dao.AuditLogDao
import com.yach.kinetix.backoffice.data.entity.AuditLogEntity
import com.yach.kinetix.core.database.TransactionRunner
import com.yach.kinetix.treasury.data.dao.CashMovementDao
import com.yach.kinetix.treasury.data.dao.CashShiftDao
import com.yach.kinetix.treasury.data.dao.SupplierDao
import com.yach.kinetix.treasury.data.entity.CashMovementEntity
import com.yach.kinetix.treasury.data.entity.CashMovementType
import com.yach.kinetix.treasury.data.entity.SupplierAccountMovementEntity
import com.yach.kinetix.treasury.data.entity.SupplierMovementType
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class RegisterSupplierPaymentUseCase @Inject constructor(
    private val supplierDao: SupplierDao,
    private val cashShiftDao: CashShiftDao,
    private val cashMovementDao: CashMovementDao,
    private val auditLogDao: AuditLogDao,
    private val transactionRunner: TransactionRunner
) {

    /**
     * Registra un pago o abono a la cuenta de un proveedor disminuyendo la deuda.
     * Si se paga en efectivo, extrae el dinero de la caja activa.
     */
    suspend operator fun invoke(
        supplierId: UUID,
        amount: BigDecimal,
        isCashPayment: Boolean,
        notes: String?,
        authorizedByUserId: UUID
    ): Result<Unit> = runCatching {
        require(amount > BigDecimal.ZERO) { "El monto del pago debe ser mayor a cero" }

        transactionRunner.runInTransaction {
            val supplier = supplierDao.getSupplierById(supplierId)
                ?: throw IllegalArgumentException("Proveedor no encontrado")

            // 1. Registrar abono en cuenta de proveedor
            val movement = SupplierAccountMovementEntity(
                id = UUID.randomUUID(),
                supplierId = supplierId,
                type = SupplierMovementType.PAYMENT_ABONO,
                amount = amount,
                notes = notes,
                registeredByUserId = authorizedByUserId,
                timestamp = Instant.now()
            )
            supplierDao.insertMovement(movement)

            // Reducir deuda con proveedor (restando amount)
            supplierDao.adjustDebt(supplierId, amount.negate())

            // 2. Si se pagó en efectivo, egresar de caja
            if (isCashPayment) {
                val activeShift = cashShiftDao.getActiveShiftSync()
                if (activeShift != null) {
                    cashMovementDao.insertMovement(
                        CashMovementEntity(
                            id = UUID.randomUUID(),
                            shiftId = activeShift.id,
                            type = CashMovementType.CASH_OUT,
                            amount = amount,
                            reason = "Pago a proveedor ${supplier.name}: ${notes ?: ""}",
                            authorizedByUserId = authorizedByUserId,
                            timestamp = Instant.now()
                        )
                    )
                }
            }

            auditLogDao.insertLog(
                AuditLogEntity(
                    userId = authorizedByUserId,
                    action = "SUPPLIER_PAYMENT",
                    entityName = "Supplier",
                    entityId = supplierId.toString(),
                    details = "Abono de $$amount a proveedor ${supplier.name}. Saldo pendiente: $${supplier.currentDebt.subtract(amount)}"
                )
            )
        }
    }
}
