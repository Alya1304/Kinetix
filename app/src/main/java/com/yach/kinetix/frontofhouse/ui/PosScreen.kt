package com.yach.kinetix.frontofhouse.ui
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yach.kinetix.backoffice.data.entity.CategoryEntity
import com.yach.kinetix.backoffice.data.entity.ProductEntity
import com.yach.kinetix.core.ui.theme.KinetixTheme
import com.yach.kinetix.frontofhouse.data.entity.PaymentMethod
import java.math.BigDecimal
import java.util.UUID
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextAlign
import com.yach.kinetix.R
import androidx.compose.ui.hapticfeedback.HapticFeedbackType



/**
 * Fase 2: Lógica y Estado
 * El layout estático ahora se conecta al PosViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(viewModel: PosViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PosScreenContent(
        uiState = uiState,
        onCategorySelect = viewModel::selectCategory,
        onProductSelect = { viewModel.addToCart(it) },
        onRemoveItem = viewModel::removeFromCart,
        onClearCart = viewModel::clearCart,
        onCheckout = {
            viewModel.checkoutDirectSale(PaymentMethod.CASH, BigDecimal.ZERO, UUID.randomUUID())
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PosScreenContent(
    uiState: PosUiState,
    onCategorySelect: (UUID?) -> Unit,
    onProductSelect: (ProductEntity) -> Unit,
    onRemoveItem: (UUID) -> Unit,
    onClearCart: () -> Unit,
    onCheckout: () -> Unit
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isCompactScreen = configuration.screenWidthDp < 800
    var showCartModal by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isCompactScreen) {
            Column(modifier = Modifier.fillMaxSize()) {
                MenuArea(
                    modifier = Modifier.fillMaxSize(),
                    uiState = uiState,
                    onCategorySelect = onCategorySelect,
                    onProductSelect = onProductSelect
                )
            }

            // Barra flotante estilo app para meseros en teléfono
            AnimatedVisibility(
                visible = uiState.cartItems.isNotEmpty(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Surface(
                    onClick = { showCartModal = true },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.onPrimary,
                                contentColor = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = "${uiState.cartItems.sumOf { it.quantity.toInt() }}",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Ver Comanda",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "$${uiState.totalAmount}",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (showCartModal) {
                ModalBottomSheet(
                    onDismissRequest = { showCartModal = false }
                ) {
                    OrderTicket(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.85f)
                            .padding(horizontal = 16.dp),
                        uiState = uiState,
                        onRemoveItem = onRemoveItem,
                        onClearCart = {
                            onClearCart()
                            showCartModal = false
                        },
                        onCheckout = {
                            onCheckout()
                            showCartModal = false
                        }
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                MenuArea(
                    modifier = Modifier.weight(0.63f),
                    uiState = uiState,
                    onCategorySelect = onCategorySelect,
                    onProductSelect = onProductSelect
                )
                OrderTicket(
                    modifier = Modifier.weight(0.37f),
                    uiState = uiState,
                    onRemoveItem = onRemoveItem,
                    onClearCart = onClearCart,
                    onCheckout = onCheckout
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MenuArea(
    modifier: Modifier = Modifier,
    uiState: PosUiState,
    onCategorySelect: (UUID?) -> Unit,
    onProductSelect: (ProductEntity) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(16.dp)
    ) {
        // Encabezado
        Text(
            text = "Menú",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Categorías
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = uiState.selectedCategoryId == null,
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onCategorySelect(null) 
                    },
                    label = { Text("Todo", style = MaterialTheme.typography.labelLarge) }
                )
            }
            items(uiState.categories, key = { it.id }) { category ->
                FilterChip(
                    modifier = Modifier.animateItem(),
                    selected = uiState.selectedCategoryId == category.id,
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onCategorySelect(category.id) 
                    },
                    label = { Text(category.name, style = MaterialTheme.typography.labelLarge) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        val filteredProducts = if (uiState.selectedCategoryId == null) {
            uiState.products
        } else {
            uiState.products.filter { it.categoryId == uiState.selectedCategoryId }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 130.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(filteredProducts, key = { it.id }) { product ->
                Card(
                    modifier = Modifier
                        .height(110.dp)
                        .animateItem()
                        .clickable { 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onProductSelect(product) 
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$${product.finalPriceWithTax}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            // Ícono decorativo tipo "Añadir"
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("+", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OrderTicket(
    modifier: Modifier = Modifier,
    uiState: PosUiState,
    onRemoveItem: (UUID) -> Unit,
    onClearCart: () -> Unit,
    onCheckout: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 16.dp)
            .padding(end = 16.dp)
            .clip(RoundedCornerShape(24.dp)) // Más redondeado
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Cabecera
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Orden Nueva", style = MaterialTheme.typography.titleLarge)
            
            AnimatedVisibility(visible = uiState.cartItems.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClearCart()
                }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Limpiar", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        
        // Lista de platillos
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(uiState.cartItems, key = { it.product.id }) { item ->
                Row(
                    modifier = Modifier.fillMaxWidth().animateItem(), 
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Círculo de cantidad
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${item.quantity}", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge)
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.product.name, style = MaterialTheme.typography.titleMedium)
                        Text("$${item.lineTotal}", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                    }
                    IconButton(onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onRemoveItem(item.product.id) 
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            if(uiState.cartItems.isEmpty()) {
                item {
                    Text(
                        text = "El carrito está vacío.\n¡Agrega algunos platillos!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp)
                    )
                }
            }
        }
        
        // Área de Cobro Final
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                .padding(20.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Subtotal (Base)", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                Text("$${uiState.baseAmount}", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("IVA Trasladado (16%)", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                Text("$${uiState.taxAmount}", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("$${uiState.totalAmount}", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCheckout()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                enabled = uiState.cartItems.isNotEmpty(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Text("COBRAR TICKET", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            AnimatedVisibility(visible = uiState.userMessage != null) {
                if (uiState.userMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(uiState.userMessage, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}


// Previsualizaciones para Android Studio
@Preview(widthDp = 1280, heightDp = 800, showBackground = true, name = "Tablet Landscape Dark")
@Composable
fun PosScreenPreviewDark() {
    KinetixTheme(darkTheme = true) {
        PosScreenContent(
            uiState = PosUiState(),
            onCategorySelect = {},
            onProductSelect = {},
            onRemoveItem = {},
            onClearCart = {},
            onCheckout = {}
        )
    }
}

@Preview(device = "spec:width=411dp,height=891dp", showBackground = true, name = "Phone Portrait Light")
@Composable
fun PosScreenPreviewMobile() {
    KinetixTheme(darkTheme = false) {
        PosScreenContent(
            uiState = PosUiState(),
            onCategorySelect = {},
            onProductSelect = {},
            onRemoveItem = {},
            onClearCart = {},
            onCheckout = {}
        )
    }
}

