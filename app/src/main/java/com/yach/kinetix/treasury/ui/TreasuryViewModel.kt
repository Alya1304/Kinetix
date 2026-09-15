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
    val declaredCardInput: String = ""
)

@HiltViewModel
class TreasuryViewModel @Inject constructor(
    private val cashShiftDao: CashShiftDao,
    private val cashMovementDao: CashMovementDao,
    private val openShiftUseCase: OpenShiftUseCase,
    private val registerCashMovementUseCase: RegisterCashMovementUseCase,
    private val blindCloseShiftUseCase: BlindCloseShiftUseCase
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
        cashierUserId: UUID,
        notes: String? = null
    ) {
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
        _uiState.update { it.copy(isPrinting = true) }
    }

    fun finishPrinting() {
        _uiState.update { it.copy(isPrinting = false) }
    }
}
