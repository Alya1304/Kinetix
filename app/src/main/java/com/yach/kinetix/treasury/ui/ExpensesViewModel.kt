package com.yach.kinetix.treasury.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yach.kinetix.frontofhouse.data.entity.PaymentMethod
import com.yach.kinetix.treasury.data.dao.ExpenseDao
import com.yach.kinetix.treasury.data.dao.SupplierDao
import com.yach.kinetix.treasury.data.entity.ExpenseCategoryEntity
import com.yach.kinetix.treasury.data.entity.ExpenseEntity
import com.yach.kinetix.treasury.data.entity.ExpenseStatus
import com.yach.kinetix.treasury.data.entity.SupplierEntity
import com.yach.kinetix.treasury.domain.RegisterExpenseUseCase
import com.yach.kinetix.treasury.domain.RegisterSupplierPaymentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

data class ExpensesUiState(
    val expenses: List<ExpenseEntity> = emptyList(),
    val pendingExpenses: List<ExpenseEntity> = emptyList(),
    val categories: List<ExpenseCategoryEntity> = emptyList(),
    val suppliers: List<SupplierEntity> = emptyList(),
    val isLoading: Boolean = false,
    val feedbackMessage: String? = null
)

@HiltViewModel
class ExpensesViewModel @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val supplierDao: SupplierDao,
    private val registerExpenseUseCase: RegisterExpenseUseCase,
    private val registerSupplierPaymentUseCase: RegisterSupplierPaymentUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpensesUiState())
    val uiState: StateFlow<ExpensesUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            expenseDao.getAllExpenses().collect { list ->
                _uiState.update { it.copy(expenses = list) }
            }
        }
        viewModelScope.launch {
            expenseDao.getPendingExpenses().collect { list ->
                _uiState.update { it.copy(pendingExpenses = list) }
            }
        }
        viewModelScope.launch {
            expenseDao.getAllActiveCategories().collect { list ->
                _uiState.update { it.copy(categories = list) }
            }
        }
        viewModelScope.launch {
            supplierDao.getAllActiveSuppliers().collect { list ->
                _uiState.update { it.copy(suppliers = list) }
            }
        }
    }

    fun registerExpense(
        categoryId: UUID,
        supplierId: UUID?,
        amount: BigDecimal,
        dueDate: Instant?,
        paymentMethod: PaymentMethod?,
        status: ExpenseStatus,
        notes: String?,
        userId: UUID
    ) {
        viewModelScope.launch {
            registerExpenseUseCase(categoryId, supplierId, amount, dueDate, paymentMethod, status, notes, userId)
                .onSuccess {
                    _uiState.update { it.copy(feedbackMessage = "Gasto por $$amount registrado exitosamente") }
                }.onFailure { err ->
                    _uiState.update { it.copy(feedbackMessage = "Error al registrar gasto: ${err.message}") }
                }
        }
    }

    fun registerSupplierPayment(
        supplierId: UUID,
        amount: BigDecimal,
        isCash: Boolean,
        notes: String?,
        userId: UUID
    ) {
        viewModelScope.launch {
            registerSupplierPaymentUseCase(supplierId, amount, isCash, notes, userId)
                .onSuccess {
                    _uiState.update { it.copy(feedbackMessage = "Abono a proveedor registrado correctamente") }
                }.onFailure { err ->
                    _uiState.update { it.copy(feedbackMessage = "Error al abonar a proveedor: ${err.message}") }
                }
        }
    }

    fun createCategory(name: String) {
        viewModelScope.launch {
            expenseDao.insertCategory(ExpenseCategoryEntity(name = name))
        }
    }

    fun createSupplier(name: String, contact: String?, phone: String?, category: String?) {
        viewModelScope.launch {
            supplierDao.insertSupplier(
                SupplierEntity(name = name, contactName = contact, phone = phone, category = category)
            )
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }
}
