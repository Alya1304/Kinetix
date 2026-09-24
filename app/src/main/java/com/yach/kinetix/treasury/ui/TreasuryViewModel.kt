package com.yach.kinetix.treasury.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yach.kinetix.treasury.data.dao.CashMovementDao
import com.yach.kinetix.treasury.data.dao.CashShiftDao
import com.yach.kinetix.treasury.data.entity.CashMovementEntity
import com.yach.kinetix.treasury.data.entity.CashMovementType
import com.yach.kinetix.treasury.data.entity.CashShiftEntity
import com.yach.kinetix.treasury.domain.BlindCloseResult
import com.yach.kinetix.treasury.domain.BlindCloseShiftUseCase
import com.yach.kinetix.treasury.domain.OpenShiftUseCase
import com.yach.kinetix.treasury.domain.RegisterCashMovementUseCase
import com.yach.kinetix.core.hardware.PrinterService
import com.yach.kinetix.core.security.SessionManager
import com.yach.kinetix.core.hardware.HardwareSettingsManager
import com.yach.kinetix.frontofhouse.data.dao.OrderDao
import com.yach.kinetix.frontofhouse.data.entity.PaymentMethod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject

enum class UserRole { ADMIN, CASHIER }

data class TreasuryUiState(
    val activeShift: CashShiftEntity? = null,
    val movements: List<CashMovementEntity> = emptyList(),
    val closeResult: BlindCloseResult? = null,
    val isLoading: Boolean = false,
    val userMessage: String? = null,
    // Campos visuales para la previsualización del diseño
    val selectedTabIndex: Int = 0,
    val currentUserRole: UserRole = UserRole.CASHIER,
    val isPrinting: Boolean = false,
    val declaredCashInput: String = "",
    val declaredCardInput: String = "",
    // Campos Dinámicos para el Dashboard
    val grossSales: BigDecimal = BigDecimal.ZERO,
    val discounts: BigDecimal = BigDecimal.ZERO,
    val tips: BigDecimal = BigDecimal.ZERO,
    val netSales: BigDecimal = BigDecimal.ZERO,
    val totalCashSales: BigDecimal = BigDecimal.ZERO,
    val totalCardSales: BigDecimal = BigDecimal.ZERO,
    val totalTransferSales: BigDecimal = BigDecimal.ZERO
)

@HiltViewModel
class TreasuryViewModel @Inject constructor(
    private val cashShiftDao: CashShiftDao,
    private val cashMovementDao: CashMovementDao,
    private val orderDao: OrderDao,
    private val openShiftUseCase: OpenShiftUseCase,
    private val registerCashMovementUseCase: RegisterCashMovementUseCase,
    private val blindCloseShiftUseCase: BlindCloseShiftUseCase,
    private val printerService: PrinterService,
    private val sessionManager: SessionManager,
    private val settingsManager: HardwareSettingsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(TreasuryUiState())
    val uiState: StateFlow<TreasuryUiState> = _uiState.asStateFlow()

    init {
        observeActiveShift()
    }

    private fun observeActiveShift() {
        viewModelScope.launch {
            cashShiftDao.getActiveShift().collect { shift ->
                _uiState.update { it.copy(activeShift = shift) }
                if (shift != null) {
                    observeMovements(shift.id)
                    observeOrders(shift.id)
                }
            }
        }
    }

    private fun observeMovements(shiftId: UUID) {
        viewModelScope.launch {
            cashMovementDao.getMovementsForShift(shiftId).collect { movements ->
                _uiState.update { it.copy(movements = movements) }
            }
        }
    }

    private fun observeOrders(shiftId: UUID) {
        viewModelScope.launch {
            orderDao.getOrdersForShift(shiftId).collect { orders ->
                val paidOrders = orders.filter { it.isPaid }
                var grossSales = BigDecimal.ZERO
                var discounts = BigDecimal.ZERO
                var tips = BigDecimal.ZERO
                var totalCashSales = BigDecimal.ZERO
                var totalCardSales = BigDecimal.ZERO
                var totalTransferSales = BigDecimal.ZERO

                for (order in paidOrders) {
                    grossSales = grossSales.add(order.totalAmount)
                    discounts = discounts.add(order.discountAmount)
                    tips = tips.add(order.tipAmount)

                    when (order.paymentMethod) {
                        PaymentMethod.CASH -> totalCashSales = totalCashSales.add(order.totalAmount)
                        PaymentMethod.CARD -> totalCardSales = totalCardSales.add(order.totalAmount)
                        PaymentMethod.TRANSFER -> totalTransferSales = totalTransferSales.add(order.totalAmount)
                        else -> {}
                    }
                }

                _uiState.update { 
                    it.copy(
                        grossSales = grossSales,
                        discounts = discounts,
                        tips = tips,
                        netSales = grossSales.subtract(discounts),
                        totalCashSales = totalCashSales,
                        totalCardSales = totalCardSales,
                        totalTransferSales = totalTransferSales
                    ) 
                }
            }
        }
    }

    fun openShift(cashierUserId: UUID, initialCash: BigDecimal) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, userMessage = null) }
            openShiftUseCase(cashierUserId, initialCash)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(isLoading = false, userMessage = "Turno abierto correctamente con $$initialCash")
                    }
                }.onFailure { error ->
                    _uiState.update { state ->
                        state.copy(isLoading = false, userMessage = "Error al abrir turno: ${error.message}")
                    }
                }
        }
    }

    fun registerMovement(
        type: CashMovementType,
        amount: BigDecimal,
        reason: String,
        authorizedByUserId: UUID
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, userMessage = null) }
            registerCashMovementUseCase(type, amount, reason, authorizedByUserId)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(isLoading = false, userMessage = "Movimiento $type por $$amount registrado con éxito")
                    }
                }.onFailure { error ->
                    _uiState.update { state ->
                        state.copy(isLoading = false, userMessage = "Error al registrar movimiento: ${error.message}")
                    }
                }
        }
    }

    fun performBlindClose(
        declaredCash: BigDecimal,
        declaredCards: BigDecimal,
        notes: String? = null
    ) {
        val cashierUserId = sessionManager.currentUser.value?.id ?: java.util.UUID.randomUUID()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, userMessage = null) }
            blindCloseShiftUseCase(declaredCash, declaredCards, cashierUserId, notes)
                .onSuccess { result ->
                    val statusText = when {
                        result.isBalanced -> "Cuadre exacto"
                        result.isShortage -> "Faltante de caja: $${result.differenceCash}"
                        else -> "Sobrante de caja: +$${result.differenceCash}"
                    }
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            closeResult = result,
                            userMessage = "Cierre ciego finalizado: $statusText"
                        )
                    }
                    printReport()
                }.onFailure { error ->
                    _uiState.update { state ->
                        state.copy(isLoading = false, userMessage = "Error al cerrar turno: ${error.message}")
                    }
                }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    // --- Métodos Visuales (Mock/Preview) ---
    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTabIndex = index) }
    }

    fun toggleRole() {
        _uiState.update { state ->
            val newRole = if (state.currentUserRole == UserRole.ADMIN) UserRole.CASHIER else UserRole.ADMIN
            state.copy(currentUserRole = newRole)
        }
    }

    fun updateDeclaredCash(amount: String) {
        if (amount.isEmpty() || amount.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
            _uiState.update { it.copy(declaredCashInput = amount) }
        }
    }

    fun updateDeclaredCard(amount: String) {
        if (amount.isEmpty() || amount.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
            _uiState.update { it.copy(declaredCardInput = amount) }
        }
    }

    fun printReport() {
        val result = _uiState.value.closeResult ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isPrinting = true) }
            val address = settingsManager.printerAddress
            if (address.isBlank()) {
                _uiState.update { it.copy(isPrinting = false, userMessage = "Impresora no configurada") }
                return@launch
            }

            printerService.connect(address).onSuccess {
                val sb = StringBuilder()
                sb.append("${settingsManager.restaurantName}\n")
                sb.append("CORTE DE CAJA (X)\n")
                sb.append("------------------------\n")
                sb.append("Efectivo Declarado: $${result.declaredCash}\n")
                sb.append("------------------------\n")
                sb.append("Ventas Efectivo: $${result.totalCashSales}\n")
                sb.append("Ventas Tarjetas: $${result.totalCardSales}\n")
                sb.append("------------------------\n")
                sb.append("Diferencia Efectivo: $${result.differenceCash}\n")
                if (result.isBalanced) {
                    sb.append("ESTADO: CUADRADO\n")
                } else if (result.isShortage) {
                    sb.append("ESTADO: FALTANTE\n")
                } else {
                    sb.append("ESTADO: SOBRANTE\n")
                }
                sb.append("\n\n\n\n\n")
                printerService.printTicket(sb.toString().toByteArray())
                printerService.disconnect()

                _uiState.update { it.copy(isPrinting = false, userMessage = "Reporte impreso") }
            }.onFailure { err ->
                _uiState.update { it.copy(isPrinting = false, userMessage = "Error de impresión: ${err.message}") }
            }
        }
    }

    fun finishPrinting() {
        _uiState.update { it.copy(isPrinting = false) }
    }
}
