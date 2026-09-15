package com.yach.kinetix.frontofhouse.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.RoomService
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yach.kinetix.frontofhouse.data.entity.OrderItemEntity
import com.yach.kinetix.frontofhouse.data.entity.PaymentMethod
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitCheckScreen(
    viewModel: SplitCheckViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val isTabletOrLandscape = configuration.screenWidthDp >= 600

    val dummyCashierId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var tipPercentage by remember { mutableStateOf(BigDecimal("0.10")) } // 10% por defecto

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.resultMessage) {
        uiState.resultMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.RoomService, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Cobro Fraccionado (Split Check)", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Barra horizontal para seleccionar la comanda activa a fraccionar
            if (uiState.activeOrders.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text("Selecciona la comanda a fraccionar:", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(uiState.activeOrders, key = { it.order.id }) { orderWithDetails ->
                                val isSelected = uiState.orderId == orderWithDetails.order.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.loadOrder(orderWithDetails.order.id)
                                    },
                                    label = { Text("Orden #${orderWithDetails.order.orderNumber} (${orderWithDetails.items.size} ítems)") },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.pendingItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No hay ítems pendientes de cobro", style = MaterialTheme.typography.titleLarge)
                        Text("Selecciona una comanda abierta arriba para fraccionar el ticket.", color = Color.Gray)
                    }
                }
            } else {
                var showSplitPaymentSheet by remember { mutableStateOf(false) }

                if (isTabletOrLandscape) {
                    // Modo Tablet / Terminal POS Horizontal (2 Columnas)
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Columna Izquierda: Comanda Original (Partidas pendientes)
                        Card(
                            modifier = Modifier
                                .weight(0.55f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                Text("Partidas de la Comanda", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text("Toca los platillos para moverlos a la fracción a cobrar", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                Spacer(modifier = Modifier.height(12.dp))

                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(uiState.pendingItems, key = { it.id }) { item ->
                                        SplitCheckItemRow(
                                            item = item,
                                            isSelected = uiState.selectedItemIds.contains(item.id),
                                            onToggle = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                viewModel.toggleItemSelection(item.id)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Columna Derecha: Fracción Seleccionada & Pasarela de Cobro
                        Card(
                            modifier = Modifier
                                .weight(0.45f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            SplitCheckoutPanel(
                                uiState = uiState,
                                selectedPaymentMethod = selectedPaymentMethod,
                                onPaymentMethodChange = { selectedPaymentMethod = it },
                                tipPercentage = tipPercentage,
                                onTipPercentageChange = { tipPercentage = it },
                                onPay = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val tipAmount = uiState.selectedTotal.multiply(tipPercentage).setScale(2, RoundingMode.HALF_UP)
                                    viewModel.paySplitCheck(selectedPaymentMethod, tipAmount, dummyCashierId)
                                }
                            )
                        }
                    }
                } else {
                    // Modo Mobile-First: Pantalla completa de platillos con barra flotante de cobro
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Toca los platillos a cobrar en esta cuenta:",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = if (uiState.selectedItemIds.isNotEmpty()) 80.dp else 0.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(uiState.pendingItems, key = { it.id }) { item ->
                                    SplitCheckItemRow(
                                        item = item,
                                        isSelected = uiState.selectedItemIds.contains(item.id),
                                        onToggle = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.toggleItemSelection(item.id)
                                        }
                                    )
                                }
                            }
                        }

                        // Barra flotante inferior para cobro en móvil
                        androidx.compose.animation.AnimatedVisibility(
                            visible = uiState.selectedItemIds.isNotEmpty(),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                        ) {
                            Surface(
                                onClick = { showSplitPaymentSheet = true },
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
                                                text = "${uiState.selectedItemIds.size}",
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Cobrar Fracción",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = "$${uiState.selectedTotal}",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (showSplitPaymentSheet) {
                            ModalBottomSheet(
                                onDismissRequest = { showSplitPaymentSheet = false }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(0.85f)
                                        .padding(16.dp)
                                ) {
                                    SplitCheckoutPanel(
                                        uiState = uiState,
                                        selectedPaymentMethod = selectedPaymentMethod,
                                        onPaymentMethodChange = { selectedPaymentMethod = it },
                                        tipPercentage = tipPercentage,
                                        onTipPercentageChange = { tipPercentage = it },
                                        onPay = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            val tipAmount = uiState.selectedTotal.multiply(tipPercentage).setScale(2, RoundingMode.HALF_UP)
                                            viewModel.paySplitCheck(selectedPaymentMethod, tipAmount, dummyCashierId)
                                            showSplitPaymentSheet = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SplitCheckItemRow(
    item: OrderItemEntity,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggle() }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("${item.quantity}x ${item.productName}", fontWeight = FontWeight.SemiBold)
                    if (!item.itemNotes.isNullOrBlank()) {
                        Text("Nota: ${item.itemNotes}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
            Text(
                text = "$${item.subtotalWithTax}",
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun SplitCheckoutPanel(
    uiState: SplitCheckUiState,
    selectedPaymentMethod: PaymentMethod,
    onPaymentMethodChange: (PaymentMethod) -> Unit,
    tipPercentage: BigDecimal,
    onTipPercentageChange: (BigDecimal) -> Unit,
    onPay: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("Ticket Fraccionado", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("${uiState.selectedItemIds.size} partidas seleccionadas", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))

            val subtotalSelected = uiState.selectedTotal
            val baseSelected = subtotalSelected.divide(BigDecimal("1.16"), 2, RoundingMode.HALF_UP)
            val taxSelected = subtotalSelected.subtract(baseSelected)
            val tipAmount = subtotalSelected.multiply(tipPercentage).setScale(2, RoundingMode.HALF_UP)
            val grandTotal = subtotalSelected.add(tipAmount)

            // Desglose
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Subtotal", color = Color.Gray)
                Text("$$baseSelected")
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("IVA (16%)", color = Color.Gray)
                Text("$$taxSelected")
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Propina (${(tipPercentage.multiply(BigDecimal(100))).toInt()}%)", color = Color.Gray)
                Text("$$tipAmount")
            }
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total a Cobrar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("$$grandTotal", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text("Propina sugerida:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "0%" to BigDecimal.ZERO,
                    "10%" to BigDecimal("0.10"),
                    "15%" to BigDecimal("0.15"),
                    "20%" to BigDecimal("0.20")
                ).forEach { (label, rate) ->
                    FilterChip(
                        selected = tipPercentage == rate,
                        onClick = { onTipPercentageChange(rate) },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Método de pago:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedPaymentMethod == PaymentMethod.CASH,
                    onClick = { onPaymentMethodChange(PaymentMethod.CASH) },
                    label = { Text("Efectivo") },
                    leadingIcon = { Icon(Icons.Default.Money, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                FilterChip(
                    selected = selectedPaymentMethod == PaymentMethod.CARD,
                    onClick = { onPaymentMethodChange(PaymentMethod.CARD) },
                    label = { Text("Tarjeta") },
                    leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
        }

        Button(
            onClick = onPay,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            enabled = uiState.selectedItemIds.isNotEmpty() && !uiState.isLoading,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("COBRAR FRACCIÓN ($${uiState.selectedTotal})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

