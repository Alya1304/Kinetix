package com.yach.kinetix.treasury.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreasuryScreen(
    viewModel: TreasuryViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Mostrar mensaje temporal si existe
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    if (uiState.isPrinting) {
        AlertDialog(
            onDismissRequest = { viewModel.finishPrinting() },
            title = { Text("Imprimiendo Reporte") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Conectando...")
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.finishPrinting() }) { Text("Cerrar") }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Cierre de Caja y Reportes", fontWeight = FontWeight.Bold) },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 16.dp)) {
                        Text("Vista: ", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = if (uiState.currentUserRole == UserRole.ADMIN) "Gerente (Transparente)" else "Cajero (Ciego)",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { viewModel.toggleRole() }) {
                            Icon(Icons.Default.ManageAccounts, contentDescription = "Cambiar Rol")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            TabRow(
                selectedTabIndex = uiState.selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = uiState.selectedTabIndex == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = { Text("Dashboard (Métricas)", fontWeight = if (uiState.selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = uiState.selectedTabIndex == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = { Text("Cierre de Caja (Arqueo)", fontWeight = if (uiState.selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal) }
                )
            }

            if (uiState.selectedTabIndex == 0) {
                DashboardView(uiState = uiState, onPrint = { viewModel.printReport() })
            } else {
                BlindCloseView(uiState = uiState, viewModel = viewModel)
            }
        }
    }
}

@Composable
fun DashboardView(uiState: TreasuryUiState, onPrint: () -> Unit) {
    if (uiState.currentUserRole != UserRole.ADMIN) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Acceso Restringido", style = MaterialTheme.typography.titleLarge)
                Text("El Dashboard financiero solo está disponible para Gerentes.", color = Color.Gray)
            }
        }
        return
    }

    val scrollState = rememberScrollState()
    
    val totalMethods = uiState.totalCashSales + uiState.totalCardSales + uiState.totalTransferSales
    val cashPct = if (totalMethods > BigDecimal.ZERO) uiState.totalCashSales.toFloat() / totalMethods.toFloat() else 0f
    val cardPct = if (totalMethods > BigDecimal.ZERO) uiState.totalCardSales.toFloat() / totalMethods.toFloat() else 0f
    val transferPct = if (totalMethods > BigDecimal.ZERO) uiState.totalTransferSales.toFloat() / totalMethods.toFloat() else 0f
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            KpiCard(title = "Ventas Brutas", value = "$${uiState.grossSales}", modifier = Modifier.weight(1f))
            KpiCard(title = "Descuentos", value = "- $${uiState.discounts}", color = Color.Red, modifier = Modifier.weight(1f))
        }
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            KpiCard(title = "Propinas", value = "$${uiState.tips}", modifier = Modifier.weight(1f))
            KpiCard(title = "Ventas Netas", value = "$${uiState.netSales}", color = Color(0xFF388E3C), modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Desglose por Método de Pago", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PaymentRow(label = "Efectivo", amount = "$${uiState.totalCashSales}", percentage = cashPct, color = Color(0xFF4CAF50))
                PaymentRow(label = "Tarjeta", amount = "$${uiState.totalCardSales}", percentage = cardPct, color = Color(0xFF2196F3))
                PaymentRow(label = "Transferencia", amount = "$${uiState.totalTransferSales}", percentage = transferPct, color = Color(0xFFFF9800))
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onPrint,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Icon(Icons.Default.Print, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Imprimir Reporte X (Lectura)", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun PaymentRow(label: String, amount: String, percentage: Float, color: Color) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontWeight = FontWeight.Bold)
            Text(amount)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { percentage },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
    }
}

@Composable
fun KpiCard(title: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun BlindCloseView(uiState: TreasuryUiState, viewModel: TreasuryViewModel) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Arqueo de Caja (Corte Z)", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Ingresa el monto físico contado antes de cerrar el turno.", color = Color.Gray)
        
        Spacer(modifier = Modifier.height(32.dp))

        if (uiState.currentUserRole == UserRole.ADMIN) {
            // Modo Transparente: El gerente ve el esperado
            val expectedCash = uiState.activeShift?.expectedCash ?: BigDecimal.ZERO
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total Esperado en Sistema (Efectivo)", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("$$expectedCash", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        } else {
            // Modo Ciego: Se muestra advertencia
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        "Corte Ciego Activo: Cuenta el dinero físico de la gaveta e ingresa la cantidad exacta. Las diferencias se reportarán a gerencia.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        OutlinedTextField(
            value = uiState.declaredCashInput,
            onValueChange = { viewModel.updateDeclaredCash(it) },
            label = { Text("Efectivo Declarado ($)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.headlineSmall,
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.declaredCardInput,
            onValueChange = { viewModel.updateDeclaredCard(it) },
            label = { Text("Tarjetas Declaradas ($) - Opcional") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.titleMedium,
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) }
        )

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(24.dp))
        
        val canClose = uiState.declaredCashInput.isNotBlank()
        
        Button(
            onClick = {
                val cash = uiState.declaredCashInput.toBigDecimalOrNull() ?: BigDecimal.ZERO
                val cards = uiState.declaredCardInput.toBigDecimalOrNull() ?: BigDecimal.ZERO
                viewModel.performBlindClose(cash, cards)
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = canClose,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Print, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Imprimir Corte Z y Cerrar Turno", style = MaterialTheme.typography.titleMedium)
        }
    }
}
