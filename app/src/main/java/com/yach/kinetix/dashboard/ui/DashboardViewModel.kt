package com.yach.kinetix.dashboard.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.math.BigDecimal
import javax.inject.Inject

data class TopItem(val name: String, val quantity: Int, val revenue: BigDecimal)

data class DashboardUiState(
    val grossSales: BigDecimal = BigDecimal.ZERO,
    val totalOrders: Int = 0,
    val averageTicket: BigDecimal = BigDecimal.ZERO,
    val activeTables: Int = 0,
    val freeTables: Int = 0,
    val topSellingItems: List<TopItem> = emptyList(),
    val cashRevenue: BigDecimal = BigDecimal.ZERO,
    val cardRevenue: BigDecimal = BigDecimal.ZERO,
    val transferRevenue: BigDecimal = BigDecimal.ZERO
)

@HiltViewModel
class DashboardViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
}
