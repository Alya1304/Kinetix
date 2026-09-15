package com.yach.kinetix.treasury.domain

import com.yach.kinetix.backoffice.data.dao.AuditLogDao
import com.yach.kinetix.backoffice.data.entity.AuditLogEntity
import com.yach.kinetix.core.database.TransactionRunner
import com.yach.kinetix.frontofhouse.data.entity.PaymentMethod
import com.yach.kinetix.treasury.data.dao.CashMovementDao
import com.yach.kinetix.treasury.data.dao.CashShiftDao
import com.yach.kinetix.treasury.data.dao.ExpenseDao
import com.yach.kinetix.treasury.data.dao.SupplierDao
import com.yach.kinetix.treasury.data.entity.CashMovementEntity
import com.yach.kinetix.treasury.data.entity.CashMovementType
import com.yach.kinetix.treasury.data.entity.ExpenseEntity
import com.yach.kinetix.treasury.data.entity.ExpenseStatus
import com.yach.kinetix.treasury.data.entity.SupplierAccountMovementEntity
import com.yach.kinetix.treasury.data.entity.SupplierMovementType
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class RegisterExpenseUseCase @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val supplierDao: SupplierDao,
    private val cashShiftDao: CashShiftDao,
    private val cashMovementDao: CashMovementDao,
    private val auditLogDao: AuditLogDao,
    private val transactionRunner: TransactionRunner
) {

    /**
     * Registra un gasto operativo. Si se liquida en efectivo en un turno activo,
     * descuenta el efectivo de caja automáticamente para el arqueo ciego.
     * Si es compra a crédito con proveedor, genera la deuda en su cuenta corriente.
     */
    suspend operator fun invoke(
        categoryId: UUID,
        supplierId: UUID?,
        amount: BigDecimal,
        dueDate: Instant?,
        paymentMethod: PaymentMethod?,
        status: ExpenseStatus,
        notes: String?,
        authorizedByUserId: UUID
    ): Result<ExpenseEntity> = runCatching {
        require(amount > BigDecimal.ZERO) { "El monto del gasto debe ser mayor a cero" }

        transactionRunner.runInTransaction {
            val activeShift = cashShiftDao.getActiveShiftSync()
            val expenseId = UUID.randomUUID()

            val expense = ExpenseEntity(
                id = expenseId,
                shiftId = if (status == ExpenseStatus.PAID && paymentMethod == PaymentMethod.CASH) activeShift?.id else null,
                categoryId = categoryId,
                supplierId = supplierId,
                amount = amount,
                dueDate = dueDate,
                paidDate = if (status == ExpenseStatus.PAID) Instant.now() else null,
                paymentMethod = paymentMethod,
                status = status,
                notes = notes,
                authorizedByUserId = authorizedByUserId,
                createdAt = Instant.now()
            )

            expenseDao.insertExpense(expense)

            // 1. Si se pagó en efectivo en turno activo, registrar egreso de caja
            if (status == ExpenseStatus.PAID && paymentMethod == PaymentMethod.CASH && activeShift != null) {
                cashMovementDao.insertMovement(
                    CashMovementEntity(
                        id = UUID.randomUUID(),
                        shiftId = activeShift.id,
                        type = CashMovementType.CASH_OUT,
                        amount = amount,
                        reason = "Gasto operativo: ${notes ?: "Sin notas"}",
                        authorizedByUserId = authorizedByUserId,
                        timestamp = Instant.now()
                    )
                )
            }

            // 2. Si es a crédito con proveedor (o pendiente con proveedor), registrar cuenta corriente
            if (supplierId != null && status == ExpenseStatus.PENDING) {
                supplierDao.adjustDebt(supplierId, amount)
                supplierDao.insertMovement(
                    SupplierAccountMovementEntity(
                        id = UUID.randomUUID(),
                        supplierId = supplierId,
                        expenseId = expenseId,
                        type = SupplierMovementType.CREDIT_PURCHASE,
                        amount = amount,
                        notes = notes,
                        registeredByUserId = authorizedByUserId,
                        timestamp = Instant.now()
                    )
                )
            }

            auditLogDao.insertLog(
                AuditLogEntity(
                    userId = authorizedByUserId,
                    action = "REGISTER_EXPENSE",
                    entityName = "Expense",
                    entityId = expenseId.toString(),
                    details = "Gasto registrado por $$amount. Estatus: $status. Proveedor: ${supplierId ?: "N/A"}"
                )
            )

            expense
        }
    }
}
