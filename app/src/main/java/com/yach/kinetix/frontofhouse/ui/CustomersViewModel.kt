package com.yach.kinetix.frontofhouse.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yach.kinetix.frontofhouse.data.dao.CustomerDao
import com.yach.kinetix.frontofhouse.data.entity.CustomerAccountMovementEntity
import com.yach.kinetix.frontofhouse.data.entity.CustomerEntity
import com.yach.kinetix.frontofhouse.domain.RegisterCustomerPaymentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject

data class CustomersUiState(
    val customers: List<CustomerEntity> = emptyList(),
    val selectedCustomer: CustomerEntity? = null,
    val movements: List<CustomerAccountMovementEntity> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val feedbackMessage: String? = null
)

@HiltViewModel
class CustomersViewModel @Inject constructor(
    private val customerDao: CustomerDao,
    private val registerCustomerPaymentUseCase: RegisterCustomerPaymentUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomersUiState())
    val uiState: StateFlow<CustomersUiState> = _uiState.asStateFlow()

    init {
        loadCustomers()
    }

    private fun loadCustomers() {
        viewModelScope.launch {
            customerDao.getAllActiveCustomers().collect { list ->
                _uiState.update { it.copy(customers = list) }
            }
        }
    }

    fun search(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        viewModelScope.launch {
            if (query.isBlank()) {
                customerDao.getAllActiveCustomers().collect { list ->
                    _uiState.update { it.copy(customers = list) }
                }
            } else {
                customerDao.searchCustomers(query).collect { list ->
                    _uiState.update { it.copy(customers = list) }
                }
            }
        }
    }

    fun selectCustomer(customer: CustomerEntity?) {
        _uiState.update { it.copy(selectedCustomer = customer) }
        viewModelScope.launch {
            if (customer != null) {
                customerDao.getMovementsForCustomer(customer.id).collect { movements ->
                    _uiState.update { it.copy(movements = movements) }
                }
            } else {
                _uiState.update { it.copy(movements = emptyList()) }
            }
        }
    }

    fun createCustomer(
        fullName: String,
        taxId: String?,
        phone: String?,
        email: String?,
        address: String?,
        defaultDiscountRate: BigDecimal,
        creditLimit: BigDecimal
    ) {
        viewModelScope.launch {
            val customer = CustomerEntity(
                fullName = fullName,
                taxId = taxId,
                phone = phone,
                email = email,
                address = address,
                defaultDiscountRate = defaultDiscountRate,
                creditLimit = creditLimit
            )
            customerDao.insertCustomer(customer)
            _uiState.update { it.copy(feedbackMessage = "Cliente ${customer.fullName} registrado con éxito") }
        }
    }

    fun registerPaymentAbono(
        customerId: UUID,
        amount: BigDecimal,
        isCash: Boolean,
        notes: String?,
        cashierUserId: UUID
    ) {
        viewModelScope.launch {
            registerCustomerPaymentUseCase(customerId, amount, isCash, notes, cashierUserId)
                .onSuccess {
                    _uiState.update { it.copy(feedbackMessage = "Abono de $$amount registrado correctamente") }
                }.onFailure { err ->
                    _uiState.update { it.copy(feedbackMessage = "Error al abonar: ${err.message}") }
                }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }
}
