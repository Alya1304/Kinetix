package com.yach.kinetix.backoffice.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yach.kinetix.backoffice.data.entity.CategoryEntity
import com.yach.kinetix.backoffice.data.entity.ProductEntity
import java.math.BigDecimal
import java.util.UUID

enum class BackofficeTab { CATALOG, INVENTORY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackofficeScreen(
    viewModel: BackofficeViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isCompactScreen = configuration.screenWidthDp < 800
    val haptic = LocalHapticFeedback.current

    var selectedTab by remember { mutableStateOf(BackofficeTab.CATALOG) }
    var showAddProductSheet by remember { mutableStateOf(false) }
    var selectedProductForAudit by remember { mutableStateOf<ProductEntity?>(null) }

    // Mostrar Snackbar cuando hay un mensaje
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Backoffice & Catálogo de Productos", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            if (selectedTab == BackofficeTab.CATALOG) {
                FloatingActionButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showAddProductSheet = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar Producto")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            TabRow(
                selectedTabIndex = if (selectedTab == BackofficeTab.CATALOG) 0 else 1,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == BackofficeTab.CATALOG,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedTab = BackofficeTab.CATALOG
                    },
                    text = { Text("Catálogo de Productos", fontWeight = if (selectedTab == BackofficeTab.CATALOG) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTab == BackofficeTab.INVENTORY,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedTab = BackofficeTab.INVENTORY
                    },
                    text = { Text("Gestión de Inventario & Mermas", fontWeight = if (selectedTab == BackofficeTab.INVENTORY) FontWeight.Bold else FontWeight.Normal) }
                )
            }

            Box(modifier = Modifier.weight(1f).padding(16.dp)) {
                when (selectedTab) {
                    BackofficeTab.CATALOG -> CatalogTab(products = uiState.products, categories = uiState.categories)
                    BackofficeTab.INVENTORY -> InventoryTab(
                        products = uiState.products, 
                        onProductSelect = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedProductForAudit = it 
                        }
                    )
                }
            }
        }
    }

    if (showAddProductSheet) {
        AddProductSheet(
            categories = uiState.categories,
            onDismiss = { showAddProductSheet = false },
            onSave = { categoryId, name, sku, barcode, price, tax, initialStock, isKitchen ->
                viewModel.saveProduct(categoryId, name, sku, barcode, price, tax, initialStock, isKitchen)
                showAddProductSheet = false
            }
        )
    }

    if (selectedProductForAudit != null) {
        AdjustStockSheet(
            product = selectedProductForAudit!!,
            onDismiss = { selectedProductForAudit = null },
            onAudit = { countedStock, notes ->
                viewModel.performStockAudit(selectedProductForAudit!!.id, countedStock, UUID.randomUUID(), notes)
                selectedProductForAudit = null
            },
            onWastage = { quantity, reason ->
                viewModel.registerWastage(selectedProductForAudit!!.id, quantity, reason, UUID.randomUUID())
                selectedProductForAudit = null
            }
        )
    }
}

@Composable
fun CatalogTab(products: List<ProductEntity>, categories: List<CategoryEntity>) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Catálogo de Productos",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(products, key = { it.id }) { product ->
                val categoryName = categories.find { it.id == product.categoryId }?.name ?: "Sin categoría"
                Card(
                    modifier = Modifier.height(130.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = product.name, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                            Text(text = categoryName, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "$${product.finalPriceWithTax}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Text(text = "SKU: ${product.sku}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InventoryTab(products: List<ProductEntity>, onProductSelect: (ProductEntity) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Gestión de Inventario",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(products, key = { it.id }) { product ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onProductSelect(product) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = product.name, style = MaterialTheme.typography.titleMedium)
                            Text(text = "SKU: ${product.sku}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${product.stockQuantity} en stock",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductSheet(
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (UUID, String, String, String?, BigDecimal, BigDecimal, BigDecimal, Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }
    var initialStock by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<CategoryEntity?>(categories.firstOrNull()) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text("Agregar Nuevo Producto", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre del Producto") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Precio Final") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = sku,
                    onValueChange = { sku = it },
                    label = { Text("SKU") },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = initialStock,
                    onValueChange = { initialStock = it },
                    label = { Text("Stock Inicial") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("Categoría", style = MaterialTheme.typography.titleSmall)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                items(categories, key = { it.id }) { cat ->
                    FilterChip(
                        selected = selectedCategory?.id == cat.id,
                        onClick = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedCategory = cat 
                        },
                        label = { Text(cat.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val catId = selectedCategory?.id ?: return@Button
                    val pPrice = price.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val pStock = initialStock.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    onSave(catId, name, sku, null, pPrice, BigDecimal("0.16"), pStock, false)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = name.isNotBlank() && price.isNotBlank() && sku.isNotBlank() && selectedCategory != null
            ) {
                Text("GUARDAR PRODUCTO", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdjustStockSheet(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onAudit: (BigDecimal, String) -> Unit,
    onWastage: (BigDecimal, String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var inputQuantity by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("AUDIT") } // "AUDIT" or "WASTAGE"

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text("Control de Inventario", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Text("Producto: ${product.name} (Actual: ${product.stockQuantity})", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = mode == "AUDIT",
                    onClick = { mode = "AUDIT" },
                    label = { Text("Ajustar Stock") }
                )
                FilterChip(
                    selected = mode == "WASTAGE",
                    onClick = { mode = "WASTAGE" },
                    label = { Text("Registrar Merma") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = inputQuantity,
                onValueChange = { inputQuantity = it },
                label = { Text(if (mode == "AUDIT") "Stock Real Contado" else "Cantidad de Merma") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text(if (mode == "AUDIT") "Notas (Opcional)" else "Razón de la Merma (Ej. Caducidad)") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val qty = inputQuantity.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    if (mode == "AUDIT") onAudit(qty, reason) else onWastage(qty, reason)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = inputQuantity.isNotBlank()
            ) {
                Text(if (mode == "AUDIT") "GUARDAR AJUSTE" else "CONFIRMAR MERMA", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
