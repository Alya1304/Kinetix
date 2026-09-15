package com.yach.kinetix.backoffice.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yach.kinetix.backoffice.data.dao.CategoryDao
import com.yach.kinetix.backoffice.data.dao.ProductDao
import com.yach.kinetix.backoffice.data.entity.CategoryEntity
import com.yach.kinetix.backoffice.data.entity.ProductEntity
import com.yach.kinetix.backoffice.domain.PerformStockAuditUseCase
import com.yach.kinetix.backoffice.domain.RegisterWastageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject

data class BackofficeUiState(
    val products: List<ProductEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val isLoading: Boolean = false,
    val feedbackMessage: String? = null
)

@HiltViewModel
class BackofficeViewModel @Inject constructor(
    private val productDao: ProductDao,
    private val categoryDao: CategoryDao,
    private val registerWastageUseCase: RegisterWastageUseCase,
    private val performStockAuditUseCase: PerformStockAuditUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackofficeUiState())
    val uiState: StateFlow<BackofficeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            productDao.getAllActiveProducts().collect { products ->
                _uiState.update { it.copy(products = products) }
            }
        }
        viewModelScope.launch {
            categoryDao.getAllActiveCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
    }

    fun saveProduct(
        categoryId: UUID,
        name: String,
        sku: String,
        barcode: String?,
        finalPriceWithTax: BigDecimal,
        taxRate: BigDecimal,
        initialStock: BigDecimal,
        isKitchenItem: Boolean
    ) {
        viewModelScope.launch {
            val product = ProductEntity(
                id = UUID.randomUUID(),
                categoryId = categoryId,
                name = name,
                sku = sku,
                barcode = barcode,
                finalPriceWithTax = finalPriceWithTax,
                taxRate = taxRate,
                stockQuantity = initialStock,
                isKitchenItem = isKitchenItem
            )
            productDao.insertProduct(product)
            _uiState.update { it.copy(feedbackMessage = "Producto ${product.name} guardado correctamente") }
        }
    }

    fun registerWastage(
        productId: UUID,
        quantity: BigDecimal,
        reason: String,
        userId: UUID
    ) {
        viewModelScope.launch {
            registerWastageUseCase(productId, quantity, reason, userId)
                .onSuccess {
                    _uiState.update { it.copy(feedbackMessage = "Merma registrada correctamente ($quantity unidades)") }
                }.onFailure { err ->
                    _uiState.update { it.copy(feedbackMessage = "Error al registrar merma: ${err.message}") }
                }
        }
    }

    fun performStockAudit(
        productId: UUID,
        countedStock: BigDecimal,
        userId: UUID,
        notes: String?
    ) {
        viewModelScope.launch {
            performStockAuditUseCase(productId, countedStock, userId, notes)
                .onSuccess { audit ->
                    _uiState.update { it.copy(feedbackMessage = "Inventario ajustado. Discrepancia: ${audit.difference}") }
                }.onFailure { err ->
                    _uiState.update { it.copy(feedbackMessage = "Error en auditoría: ${err.message}") }
                }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }
}
