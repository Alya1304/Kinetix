package com.yach.kinetix.frontofhouse.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yach.kinetix.frontofhouse.data.dao.OrderDao
import com.yach.kinetix.frontofhouse.data.entity.OrderItemEntity
import com.yach.kinetix.frontofhouse.data.entity.PaymentMethod
import com.yach.kinetix.frontofhouse.domain.SplitCheckPaymentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject

data class SplitCheckUiState(
    val orderId: UUID? = null,
    val activeOrders: List<com.yach.kinetix.frontofhouse.data.model.OrderWithDetails> = emptyList(),
    val pendingItems: List<OrderItemEntity> = emptyList(),
    val selectedItemIds: Set<UUID> = emptySet(),
    val selectedTotal: BigDecimal = BigDecimal.ZERO,
    val isLoading: Boolean = false,
    val resultMessage: String? = null
)

@HiltViewModel
class SplitCheckViewModel @Inject constructor(
    private val orderDao: OrderDao,
    private val splitCheckPaymentUseCase: SplitCheckPaymentUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplitCheckUiState())
    val uiState: StateFlow<SplitCheckUiState> = _uiState.asStateFlow()

    init {
        observeActiveOrders()
    }

    private fun observeActiveOrders() {
        viewModelScope.launch {
            orderDao.getActiveKitchenOrders().collect { list ->
                _uiState.update { state ->
                    val updated = state.copy(activeOrders = list)
                    if (state.orderId == null && list.isNotEmpty()) {
                        loadOrder(list.first().order.id)
                    }
                    updated
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(resultMessage = null) }
    }

    fun loadOrder(orderId: UUID) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, orderId = orderId) }
            orderDao.getOrderWithDetails(orderId).collect { details ->
                if (details != null) {
                    val pending = details.items.map { it.item }.filter { !it.isPaid }
                    _uiState.update {
                        it.copy(
                            pendingItems = pending,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun toggleItemSelection(itemId: UUID) {
        _uiState.update { state ->
            val updatedSelection = if (state.selectedItemIds.contains(itemId)) {
                state.selectedItemIds - itemId
            } else {
                state.selectedItemIds + itemId
            }

            val totalSelected = state.pendingItems
                .filter { updatedSelection.contains(it.id) }
                .fold(BigDecimal.ZERO) { acc, item -> acc.add(item.subtotalWithTax) }

            state.copy(
                selectedItemIds = updatedSelection,
                selectedTotal = totalSelected
            )
        }
    }

    fun paySplitCheck(
        paymentMethod: PaymentMethod,
        tipAmount: BigDecimal,
        cashierUserId: UUID
    ) {
        val orderId = _uiState.value.orderId ?: return
        val selectedIds = _uiState.value.selectedItemIds.toList()
        if (selectedIds.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            splitCheckPaymentUseCase(
                orderId = orderId,
                selectedOrderItemIds = selectedIds,
                paymentMethod = paymentMethod,
                tipAmount = tipAmount,
                cashierUserId = cashierUserId
            ).onSuccess { result ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        selectedItemIds = emptySet(),
                        selectedTotal = BigDecimal.ZERO,
                        resultMessage = "Cobro parcial de ${result.itemsPaidCount} ítems liquidado ($${result.amountPaid}). Orden completada: ${result.isOrderFullyPaid}"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        resultMessage = "Error en cobro fraccionado: ${error.message}"
                    )
                }
            }
        }
    }
}
