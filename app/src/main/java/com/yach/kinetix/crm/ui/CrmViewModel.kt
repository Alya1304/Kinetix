package com.yach.kinetix.crm.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import javax.inject.Inject

enum class CustomerCategory { REGULAR, VIP, DEBTOR }

data class Customer(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val phone: String,
    val email: String,
    val category: CustomerCategory,
    val loyaltyPoints: Int,
    val totalVisits: Int,
    val averageTicket: Double,
    val lastVisit: String
)

data class CrmUiState(
    val customers: List<Customer> = emptyList(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val selectedFilterIndex: Int = 0,
    val filters: List<String> = listOf("Todos", "VIP", "Deudores"),
    val selectedCustomerId: UUID? = null
)

@HiltViewModel
class CrmViewModel @Inject constructor() : ViewModel() {

    private val allCustomers = listOf(
        Customer(name = "Alejandro Torres", phone = "555-0192", email = "atorres@mail.com", category = CustomerCategory.VIP, loyaltyPoints = 1250, totalVisits = 45, averageTicket = 850.50, lastVisit = "Hace 2 días"),
        Customer(name = "Mariana Ríos", phone = "555-8831", email = "mrios@mail.com", category = CustomerCategory.REGULAR, loyaltyPoints = 300, totalVisits = 5, averageTicket = 450.00, lastVisit = "Hace 1 semana"),
        Customer(name = "Carlos Fuentes", phone = "555-3490", email = "cfuentes@mail.com", category = CustomerCategory.VIP, loyaltyPoints = 3400, totalVisits = 120, averageTicket = 1200.00, lastVisit = "Ayer"),
        Customer(name = "Roberto Ayala", phone = "555-1122", email = "rayala@mail.com", category = CustomerCategory.DEBTOR, loyaltyPoints = 50, totalVisits = 2, averageTicket = 600.00, lastVisit = "Hace 1 mes"),
        Customer(name = "Diana Silva", phone = "555-7744", email = "dsilva@mail.com", category = CustomerCategory.REGULAR, loyaltyPoints = 120, totalVisits = 3, averageTicket = 320.00, lastVisit = "Hace 3 semanas"),
        Customer(name = "Empresa ACME S.A.", phone = "555-9999", email = "eventos@acme.com", category = CustomerCategory.VIP, loyaltyPoints = 5000, totalVisits = 10, averageTicket = 4500.00, lastVisit = "Hace 5 días")
    )

    private val _uiState = MutableStateFlow(CrmUiState())
    val uiState: StateFlow<CrmUiState> = _uiState.asStateFlow()

    init {
        applyFilters()
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun onSearchActiveChange(isActive: Boolean) {
        _uiState.update { it.copy(isSearchActive = isActive) }
    }

    fun selectFilter(index: Int) {
        _uiState.update { it.copy(selectedFilterIndex = index) }
        applyFilters()
    }

    fun selectCustomer(id: UUID?) {
        _uiState.update { it.copy(selectedCustomerId = id) }
    }

    private fun applyFilters() {
        val currentState = _uiState.value
        var filteredList = allCustomers

        // Filtro por texto
        if (currentState.searchQuery.isNotBlank()) {
            val q = currentState.searchQuery.lowercase()
            filteredList = filteredList.filter {
                it.name.lowercase().contains(q) || it.phone.contains(q) || it.email.lowercase().contains(q)
            }
        }

        // Filtro por tab
        filteredList = when (currentState.selectedFilterIndex) {
            1 -> filteredList.filter { it.category == CustomerCategory.VIP }
            2 -> filteredList.filter { it.category == CustomerCategory.DEBTOR }
            else -> filteredList // 0 = Todos
        }

        _uiState.update { it.copy(customers = filteredList) }
    }
}
