package com.yach.kinetix.frontofhouse.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yach.kinetix.backoffice.data.dao.CategoryDao
import com.yach.kinetix.backoffice.data.dao.ProductDao
import com.yach.kinetix.backoffice.data.entity.CategoryEntity
import com.yach.kinetix.backoffice.data.entity.ProductEntity
import com.yach.kinetix.core.finance.TaxCalculator
import com.yach.kinetix.core.hardware.PrinterService
import com.yach.kinetix.frontofhouse.data.entity.OrderType
import com.yach.kinetix.frontofhouse.data.entity.PaymentMethod
import com.yach.kinetix.frontofhouse.domain.CreateOrderUseCase
import com.yach.kinetix.frontofhouse.domain.OrderItemParam
import com.yach.kinetix.frontofhouse.domain.ProcessSaleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject

data class CartItemUiModel(
    val product: ProductEntity,
    val quantity: BigDecimal,
    val lineTotal: BigDecimal,
    val lineBase: BigDecimal,
    val lineTax: BigDecimal
)

data class PosUiState(
    val categories: List<CategoryEntity> = emptyList(),
    val products: List<ProductEntity> = emptyList(),
    val selectedCategoryId: UUID? = null,
    val cartItems: List<CartItemUiModel> = emptyList(),
    val totalAmount: BigDecimal = BigDecimal.ZERO,
    val baseAmount: BigDecimal = BigDecimal.ZERO,
    val taxAmount: BigDecimal = BigDecimal.ZERO,
    val selectedTableId: UUID? = null,
    val selectedOrderType: OrderType = OrderType.DINE_IN,
    val isLoading: Boolean = false,
    val userMessage: String? = null,
    val isPrinterConnected: Boolean = false
)

@HiltViewModel
class PosViewModel @Inject constructor(
    private val categoryDao: CategoryDao,
    private val productDao: ProductDao,
    private val createOrderUseCase: CreateOrderUseCase,
    private val processSaleUseCase: ProcessSaleUseCase,
    private val printerService: PrinterService
) : ViewModel() {

    private val _uiState = MutableStateFlow(PosUiState())
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

    init {
        loadCatalog()
    }

    private fun loadCatalog() {
        viewModelScope.launch {
            categoryDao.getAllActiveCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
        viewModelScope.launch {
            productDao.getAllActiveProducts().collect { products ->
                _uiState.update { it.copy(products = products) }
            }
        }
    }

    fun selectCategory(categoryId: UUID?) {
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
    }

    fun addToCart(product: ProductEntity, quantity: BigDecimal = BigDecimal.ONE) {
        _uiState.update { state ->
            val existing = state.cartItems.find { it.product.id == product.id }
            val newQuantity = existing?.quantity?.add(quantity) ?: quantity

            val breakdown = TaxCalculator.calculateLineItemBreakdown(
                unitPriceWithTax = product.finalPriceWithTax,
                modifiersExtraWithTax = BigDecimal.ZERO,
                quantity = newQuantity,
                taxRate = product.taxRate
            )

            val updatedItem = CartItemUiModel(
                product = product,
                quantity = newQuantity,
                lineTotal = breakdown.finalTotal,
                lineBase = breakdown.baseAmount,
                lineTax = breakdown.taxAmount
            )

            val newCart = state.cartItems.filterNot { it.product.id == product.id } + updatedItem

            val newTotal = newCart.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.lineTotal) }
            val newBase = newCart.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.lineBase) }
            val newTax = newCart.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.lineTax) }

            state.copy(
                cartItems = newCart,
                totalAmount = newTotal,
                baseAmount = newBase,
                taxAmount = newTax
            )
        }
    }

    fun removeFromCart(productId: UUID) {
        _uiState.update { state ->
            val newCart = state.cartItems.filterNot { it.product.id == productId }
            val newTotal = newCart.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.lineTotal) }
            val newBase = newCart.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.lineBase) }
            val newTax = newCart.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.lineTax) }

            state.copy(
                cartItems = newCart,
                totalAmount = newTotal,
                baseAmount = newBase,
                taxAmount = newTax
            )
        }
    }

    fun clearCart() {
        _uiState.update {
            it.copy(
                cartItems = emptyList(),
                totalAmount = BigDecimal.ZERO,
                baseAmount = BigDecimal.ZERO,
                taxAmount = BigDecimal.ZERO
            )
        }
    }

    fun checkoutDirectSale(
        paymentMethod: PaymentMethod,
        tipAmount: BigDecimal,
        cashierUserId: UUID
    ) {
        val currentState = _uiState.value
        if (currentState.cartItems.isEmpty()) {
            _uiState.update { it.copy(userMessage = "El carrito se encuentra vacío") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, userMessage = null) }

            val itemParams = currentState.cartItems.map {
                OrderItemParam(
                    productId = it.product.id,
                    quantity = it.quantity
                )
            }

            // 1. Crear orden
            createOrderUseCase(
                tableId = currentState.selectedTableId,
                serverUserId = cashierUserId,
                orderType = currentState.selectedOrderType,
                itemsParams = itemParams
            ).onSuccess { createdOrder ->
                // 2. Liquidar venta directamente
                processSaleUseCase(
                    orderId = createdOrder.id,
                    paymentMethod = paymentMethod,
                    tipAmount = tipAmount,
                    cashierUserId = cashierUserId
                ).onSuccess {
                    clearCart()
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            userMessage = "Venta procesada con éxito. Folio: #${createdOrder.orderNumber}"
                        )
                    }
                }.onFailure { err ->
                    _uiState.update { state ->
                        state.copy(isLoading = false, userMessage = "Error al liquidar cobro: ${err.message}")
                    }
                }
            }.onFailure { err ->
                _uiState.update { state ->
                    state.copy(isLoading = false, userMessage = "Error al generar orden: ${err.message}")
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
