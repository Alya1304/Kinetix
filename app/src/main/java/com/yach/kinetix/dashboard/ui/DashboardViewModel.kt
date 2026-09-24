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
    val grossSales: BigDecimal = BigDecimal("12450.00"),
    val totalOrders: Int = 45,
    val averageTicket: BigDecimal = BigDecimal("276.50"),
    val activeTables: Int = 8,
    val freeTables: Int = 4,
    val topSellingItems: List<TopItem> = listOf(
        TopItem("Hamburguesa BBQ", 24, BigDecimal("3600.00")),
        TopItem("Limonada", 18, BigDecimal("630.00")),
        TopItem("Tacos Al Pastor", 15, BigDecimal("1500.00")),
        TopItem("Cerveza Artesanal", 12, BigDecimal("840.00")),
        TopItem("Papas Fritas", 10, BigDecimal("450.00"))
    ),
    val cashRevenue: BigDecimal = BigDecimal("4200.00"),
    val cardRevenue: BigDecimal = BigDecimal("7150.00"),
    val transferRevenue: BigDecimal = BigDecimal("1100.00")
)

@HiltViewModel
class DashboardViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
}
