package com.yach.kinetix.backoffice.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yach.kinetix.backoffice.data.dao.CategoryDao
import com.yach.kinetix.backoffice.data.dao.IngredientDao
import com.yach.kinetix.backoffice.data.dao.ProductDao
import com.yach.kinetix.backoffice.data.dao.RecipeDao
import com.yach.kinetix.backoffice.data.entity.CategoryEntity
import com.yach.kinetix.backoffice.data.entity.IngredientEntity
import com.yach.kinetix.backoffice.data.entity.ProductEntity
import com.yach.kinetix.backoffice.data.entity.RecipeItemEntity
import com.yach.kinetix.backoffice.domain.PerformStockAuditUseCase
import com.yach.kinetix.backoffice.domain.RegisterWastageUseCase
import com.yach.kinetix.frontofhouse.data.dao.ModifierDao
import com.yach.kinetix.frontofhouse.data.entity.ModifierEntity
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
    val ingredients: List<IngredientEntity> = emptyList(),
    val modifiers: List<ModifierEntity> = emptyList(),
    val recipes: Map<UUID, List<RecipeItemEntity>> = emptyMap(),
    val isLoading: Boolean = false,
    val feedbackMessage: String? = null
)

@HiltViewModel
class BackofficeViewModel @Inject constructor(
    private val productDao: ProductDao,
    private val categoryDao: CategoryDao,
    private val ingredientDao: IngredientDao,
    private val recipeDao: RecipeDao,
    private val modifierDao: ModifierDao,
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
        viewModelScope.launch {
            ingredientDao.getAllActiveIngredients().collect { ingredients ->
                _uiState.update { it.copy(ingredients = ingredients) }
            }
        }
        viewModelScope.launch {
            modifierDao.getAllActiveModifiers().collect { modifiers ->
                _uiState.update { it.copy(modifiers = modifiers) }
            }
        }
        viewModelScope.launch {
            recipeDao.getAllRecipes().collect { allItems ->
                val grouped = allItems.groupBy { it.productId }
                _uiState.update { it.copy(recipes = grouped) }
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

    fun saveIngredient(name: String, unitOfMeasure: String, costPerUnit: BigDecimal) {
        viewModelScope.launch {
            val ingredient = IngredientEntity(
                name = name,
                unitOfMeasure = unitOfMeasure,
                costPerUnit = costPerUnit
            )
            ingredientDao.insertIngredient(ingredient)
            _uiState.update { it.copy(feedbackMessage = "Insumo ${ingredient.name} guardado") }
        }
    }

    fun saveModifier(name: String, extraPrice: BigDecimal) {
        viewModelScope.launch {
            val modifier = ModifierEntity(
                name = name,
                extraPriceWithTax = extraPrice
            )
            modifierDao.insertModifier(modifier)
            _uiState.update { it.copy(feedbackMessage = "Modificador ${modifier.name} guardado") }
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

    fun addIngredientToRecipe(productId: UUID, ingredientId: UUID, requiredQuantity: BigDecimal) {
        viewModelScope.launch {
            val item = RecipeItemEntity(
                id = UUID.randomUUID(),
                productId = productId,
                ingredientId = ingredientId,
                requiredQuantity = requiredQuantity
            )
            recipeDao.insertRecipeItem(item)
            _uiState.update { it.copy(feedbackMessage = "Insumo agregado a la receta") }
        }
    }

    fun removeIngredientFromRecipe(recipeItemId: UUID) {
        viewModelScope.launch {
            recipeDao.deleteRecipeItem(recipeItemId)
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }
}
