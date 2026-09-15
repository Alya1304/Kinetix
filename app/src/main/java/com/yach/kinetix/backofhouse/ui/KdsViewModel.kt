package com.yach.kinetix.backofhouse.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yach.kinetix.backofhouse.domain.KitchenAlertManager
import com.yach.kinetix.backofhouse.domain.UpdateKitchenOrderStatusUseCase
import com.yach.kinetix.frontofhouse.data.dao.OrderDao
import com.yach.kinetix.frontofhouse.data.entity.KdsItemStatus
import com.yach.kinetix.frontofhouse.data.entity.OrderStatus
import com.yach.kinetix.frontofhouse.data.model.OrderWithDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

enum class KdsSlaStatus {
    ON_TIME,      // Verde: < 15 minutos
    DELAYED,      // Amarillo: 15 a 30 minutos
    VERY_DELAYED  // Rojo: > 30 minutos
}

data class KdsOrderCardUiModel(
    val orderDetails: OrderWithDetails,
    val minutesElapsed: Long,
    val slaStatus: KdsSlaStatus
)

data class KdsUiState(
    val orderCards: List<KdsOrderCardUiModel> = emptyList(),
    val totalActiveCount: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class KdsViewModel @Inject constructor(
    private val orderDao: OrderDao,
    private val updateKitchenOrderStatusUseCase: UpdateKitchenOrderStatusUseCase,
    private val kitchenAlertManager: KitchenAlertManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(KdsUiState())
    val uiState: StateFlow<KdsUiState> = _uiState.asStateFlow()

    private var lastOrderCount = 0

    init {
        observeKitchenOrders()
    }

    private fun observeKitchenOrders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            orderDao.getActiveKitchenOrders().collect { orders ->
                val now = Instant.now()
                val cards = orders.map { orderWithDetails ->
                    val elapsed = Duration.between(orderWithDetails.order.createdAt, now).toMinutes()
                    val sla = when {
                        elapsed < 15 -> KdsSlaStatus.ON_TIME
                        elapsed <= 30 -> KdsSlaStatus.DELAYED
                        else -> KdsSlaStatus.VERY_DELAYED
                    }
                    KdsOrderCardUiModel(
                        orderDetails = orderWithDetails,
                        minutesElapsed = elapsed,
                        slaStatus = sla
                    )
                }

                // Si entraron nuevas comandas a la pantalla, reproducir alerta sonora
                if (orders.size > lastOrderCount) {
                    kitchenAlertManager.playNewOrderSound()
                }
                lastOrderCount = orders.size

                _uiState.update {
                    it.copy(
                        orderCards = cards,
                        totalActiveCount = orders.size,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun setOrderCooking(orderId: UUID, cookUserId: UUID) {
        viewModelScope.launch {
            updateKitchenOrderStatusUseCase.updateOrderStatus(orderId, OrderStatus.IN_PREPARATION, cookUserId)
        }
    }

    fun setOrderReady(orderId: UUID, cookUserId: UUID) {
        viewModelScope.launch {
            updateKitchenOrderStatusUseCase.updateOrderStatus(orderId, OrderStatus.READY, cookUserId)
            kitchenAlertManager.playOrderReadySound()
        }
    }

    fun setItemReady(orderItemId: UUID, cookUserId: UUID) {
        viewModelScope.launch {
            updateKitchenOrderStatusUseCase.updateItemStatus(orderItemId, KdsItemStatus.READY, cookUserId)
        }
    }
}
