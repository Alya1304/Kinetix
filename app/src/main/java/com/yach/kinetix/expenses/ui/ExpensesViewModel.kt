package com.yach.kinetix.expenses.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import javax.inject.Inject

enum class ExpenseCategory { INSUMOS, OPERACION, REEMBOLSO, OTROS }

data class Expense(
    val id: UUID = UUID.randomUUID(),
    val concept: String,
    val amount: Double,
    val category: ExpenseCategory,
    val time: String
)

data class Supplier(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val category: String,
    val pendingBalance: Double,
    val lastDelivery: String
)

data class ExpensesUiState(
    val selectedTabIndex: Int = 0,
    val tabs: List<String> = listOf("Caja Chica", "Proveedores"),
    val expenses: List<Expense> = emptyList(),
    val suppliers: List<Supplier> = emptyList(),
    val selectedSupplierId: UUID? = null
)

@HiltViewModel
class ExpensesViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ExpensesUiState())
    val uiState: StateFlow<ExpensesUiState> = _uiState.asStateFlow()

    init {
        loadMockData()
    }

    private fun loadMockData() {
        val mockExpenses = listOf(
            Expense(concept = "Hielo (Urgencia)", amount = 150.0, category = ExpenseCategory.INSUMOS, time = "Hace 20 min"),
            Expense(concept = "Propinas Repartidor", amount = 50.0, category = ExpenseCategory.OPERACION, time = "Hace 1 hora"),
            Expense(concept = "Focos Led (Barra)", amount = 280.0, category = ExpenseCategory.OPERACION, time = "Hace 3 horas"),
            Expense(concept = "Limones Frescos", amount = 120.0, category = ExpenseCategory.INSUMOS, time = "Esta mañana")
        )

        val mockSuppliers = listOf(
            Supplier(name = "Coca-Cola Femsa", category = "Bebidas", pendingBalance = 4500.00, lastDelivery = "Ayer"),
            Supplier(name = "Carnicería La Selecta", category = "Carnes", pendingBalance = 0.0, lastDelivery = "Hoy (Mañana)"),
            Supplier(name = "Lácteos y Quesos S.A.", category = "Lácteos", pendingBalance = 1250.50, lastDelivery = "Hace 3 días"),
            Supplier(name = "Distribuidora de Vinos", category = "Alcohol", pendingBalance = 8400.00, lastDelivery = "Hace 1 semana")
        )

        _uiState.update { it.copy(expenses = mockExpenses, suppliers = mockSuppliers) }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTabIndex = index) }
    }

    fun selectSupplier(id: UUID?) {
        _uiState.update { it.copy(selectedSupplierId = id) }
    }

    fun addExpense(concept: String, amount: Double) {
        val newExpense = Expense(
            concept = concept,
            amount = amount,
            category = ExpenseCategory.OTROS,
            time = "Ahora mismo"
        )
        _uiState.update { it.copy(expenses = listOf(newExpense) + it.expenses) }
    }
}
