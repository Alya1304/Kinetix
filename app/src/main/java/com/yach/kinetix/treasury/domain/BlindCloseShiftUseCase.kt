package com.yach.kinetix.treasury.domain

import com.yach.kinetix.backoffice.data.dao.AuditLogDao
import com.yach.kinetix.backoffice.data.entity.AuditLogEntity
import com.yach.kinetix.core.database.TransactionRunner
import com.yach.kinetix.frontofhouse.data.dao.OrderDao
import com.yach.kinetix.frontofhouse.data.entity.PaymentMethod
import com.yach.kinetix.treasury.data.dao.CashMovementDao
import com.yach.kinetix.treasury.data.dao.CashShiftDao
import com.yach.kinetix.treasury.data.entity.CashMovementType
import com.yach.kinetix.treasury.data.entity.ShiftStatus
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

data class BlindCloseResult(
    val shiftId: UUID,
    val initialCash: BigDecimal,
    val totalCashSales: BigDecimal,
    val totalCardSales: BigDecimal,
    val totalCashIn: BigDecimal,
    val totalCashOut: BigDecimal,
    val expectedCash: BigDecimal,
    val declaredCash: BigDecimal,
    val differenceCash: BigDecimal,
    val isBalanced: Boolean,
    val isShortage: Boolean,
    val isSurplus: Boolean,
    val closedAt: Instant
)

class BlindCloseShiftUseCase @Inject constructor(
    private val cashShiftDao: CashShiftDao,
    private val cashMovementDao: CashMovementDao,
    private val orderDao: OrderDao,
    private val auditLogDao: AuditLogDao,
    private val transactionRunner: TransactionRunner
) {

    /**
     * Ejecuta el cierre ciego antifraude de turno de caja.
     * El cajero ingresa el conteo físico de dinero sin conocer los saldos del sistema,
     * calculándose automáticamente el cuadre financiero (sobrante o faltante).
     */
    suspend operator fun invoke(
        declaredCash: BigDecimal,
        declaredCards: BigDecimal,
        cashierUserId: UUID,
        notes: String? = null
    ): Result<BlindCloseResult> = runCatching {
        require(declaredCash >= BigDecimal.ZERO) { "El efectivo contado no puede ser negativo" }
        require(declaredCards >= BigDecimal.ZERO) { "El monto en tarjetas no puede ser negativo" }

        transactionRunner.runInTransaction {
            val activeShift = cashShiftDao.getActiveShiftSync()
                ?: throw IllegalStateException("No existe ningún turno de caja abierto para cerrar")

            // 1. Calcular ventas liquidadas en este turno
            val paidOrders = orderDao.getPaidOrdersForShiftSync(activeShift.id)
            var totalCashSales = BigDecimal.ZERO
            var totalCardSales = BigDecimal.ZERO

            for (order in paidOrders) {
                when (order.paymentMethod) {
                    PaymentMethod.CASH -> totalCashSales = totalCashSales.add(order.totalAmount)
                    PaymentMethod.CARD -> totalCardSales = totalCardSales.add(order.totalAmount)
                    PaymentMethod.MIXED -> {
                        // En pagos mixtos o no categorizados por defecto a efectivo
                        totalCashSales = totalCashSales.add(order.totalAmount)
                    }
                    PaymentMethod.CUSTOMER_CREDIT -> {
                        // Crédito de cliente, no cuenta como efectivo en caja
                    }
                    PaymentMethod.DELIVERY_PLATFORM -> {
                        // Pagos recibidos por plataforma externa
                    }
                    null -> {}
                }
            }

            // 2. Calcular movimientos de caja durante el turno
            val movements = cashMovementDao.getMovementsForShiftSync(activeShift.id)
            var totalCashIn = BigDecimal.ZERO
            var totalCashOut = BigDecimal.ZERO

            for (movement in movements) {
                when (movement.type) {
                    CashMovementType.CASH_IN -> totalCashIn = totalCashIn.add(movement.amount)
                    CashMovementType.CASH_OUT -> totalCashOut = totalCashOut.add(movement.amount)
                }
            }

            // 3. Fórmula del Arqueo Antifraude:
            // ExpectedCash = Fondo Inicial + Ventas Efectivo + Entradas de Efectivo - Salidas de Efectivo
            val expectedCash = activeShift.initialCash
                .add(totalCashSales)
                .add(totalCashIn)
                .subtract(totalCashOut)

            // Diferencia = Declarado - Esperado
            val differenceCash = declaredCash.subtract(expectedCash)
            val now = Instant.now()

            // 4. Actualizar y cerrar el turno en base de datos
            val closedShift = activeShift.copy(
                closedAt = now,
                declaredCash = declaredCash,
                declaredCards = declaredCards,
                expectedCash = expectedCash,
                differenceCash = differenceCash,
                status = ShiftStatus.CLOSED,
                notes = notes
            )
            cashShiftDao.updateShift(closedShift)

            // 5. Auditoría forense
            val auditDetail = buildString {
                append("Cierre ciego de turno ejecutado. ")
                append("Fondo inicial: $$${activeShift.initialCash}, ")
                append("Ventas efectivo: $$totalCashSales, ")
                append("Entradas: $$totalCashIn, Retiros: $$totalCashOut. ")
                append("Efectivo esperado: $$expectedCash | Declarado: $$declaredCash | ")
                if (differenceCash.compareTo(BigDecimal.ZERO) == 0) {
                    append("CUADRE PERFECTO")
                } else if (differenceCash < BigDecimal.ZERO) {
                    append("FALTANTE DE CAJA: $$differenceCash")
                } else {
                    append("SOBRANTE DE CAJA: +$$differenceCash")
                }
            }

            auditLogDao.insertLog(
                AuditLogEntity(
                    userId = cashierUserId,
                    action = "BLIND_CLOSE_SHIFT",
                    entityName = "CashShift",
                    entityId = activeShift.id.toString(),
                    details = auditDetail
                )
            )

            BlindCloseResult(
                shiftId = activeShift.id,
                initialCash = activeShift.initialCash,
                totalCashSales = totalCashSales,
                totalCardSales = totalCardSales,
                totalCashIn = totalCashIn,
                totalCashOut = totalCashOut,
                expectedCash = expectedCash,
                declaredCash = declaredCash,
                differenceCash = differenceCash,
                isBalanced = differenceCash.compareTo(BigDecimal.ZERO) == 0,
                isShortage = differenceCash < BigDecimal.ZERO,
                isSurplus = differenceCash > BigDecimal.ZERO,
                closedAt = now
            )
        }
    }
}
