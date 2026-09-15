package com.yach.kinetix.expenses.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    viewModel: ExpensesViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddExpenseDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Gastos y Proveedores", fontWeight = FontWeight.Bold) }
            )
        },
        floatingActionButton = {
            if (uiState.selectedTabIndex == 0) {
                ExtendedFloatingActionButton(
                    onClick = { showAddExpenseDialog = true },
                    icon = { Icon(Icons.Default.Add, "Registrar Gasto") },
                    text = { Text("Registrar Gasto") },
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Widget de Resumen Financiero
            if (uiState.selectedTabIndex == 0) {
                val totalSpent = uiState.expenses.sumOf { it.amount }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Total Gastado Hoy (Caja Chica)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "- $${"%.2f".format(totalSpent)}",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F) // Rojo para salidas
                        )
                    }
                }
            } else {
                val totalDebt = uiState.suppliers.sumOf { it.pendingBalance }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Deuda Total Proveedores", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$${"%.2f".format(totalDebt)}",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Tabs
            TabRow(
                selectedTabIndex = uiState.selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                uiState.tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = uiState.selectedTabIndex == index,
                        onClick = { viewModel.selectTab(index) },
                        text = { Text(title, fontWeight = if (uiState.selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            // Contenido
            if (uiState.selectedTabIndex == 0) {
                // Lista de Gastos
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.expenses, key = { it.id }) { expense ->
                        ExpenseItem(expense)
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) } // Para el FAB
                }
            } else {
                // Cuadrícula de Proveedores
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 300.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.suppliers, key = { it.id }) { supplier ->
                        SupplierCard(
                            supplier = supplier,
                            onClick = { viewModel.selectSupplier(supplier.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddExpenseDialog) {
        var concept by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showAddExpenseDialog = false },
            title = { Text("Registrar Gasto Rápido") },
            text = {
                Column {
                    OutlinedTextField(
                        value = concept,
                        onValueChange = { concept = it },
                        label = { Text("Concepto (ej. Propinas, Hielo)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Monto ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val num = amount.toDoubleOrNull() ?: 0.0
                        if (concept.isNotBlank() && num > 0) {
                            viewModel.addExpense(concept, num)
                            showAddExpenseDialog = false
                        }
                    },
                    enabled = concept.isNotBlank() && amount.isNotBlank()
                ) { Text("Registrar") }
            },
            dismissButton = {
                TextButton(onClick = { showAddExpenseDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Modal de Proveedor (Estado de cuenta)
    if (uiState.selectedSupplierId != null) {
        val supplier = uiState.suppliers.find { it.id == uiState.selectedSupplierId }
        if (supplier != null) {
            SupplierBottomSheet(
                supplier = supplier,
                onDismiss = { viewModel.selectSupplier(null) }
            )
        } else {
            viewModel.selectSupplier(null)
        }
    }
}

@Composable
fun ExpenseItem(expense: Expense) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(expense.concept, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(expense.time, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            
            Text(
                text = "- $${"%.2f".format(expense.amount)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD32F2F)
            )
        }
    }
}

@Composable
fun SupplierCard(supplier: Supplier, onClick: () -> Unit) {
    val hasDebt = supplier.pendingBalance > 0
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(supplier.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(supplier.category, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
                Box(
                    modifier = Modifier
                        .background(
                            if (hasDebt) Color.Red.copy(alpha = 0.1f) else Color.Green.copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (hasDebt) "Con Deuda" else "Al Corriente",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hasDebt) Color.Red else Color(0xFF388E3C),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Saldo Pendiente:", color = Color.Gray)
                Text(
                    text = "$${"%.2f".format(supplier.pendingBalance)}",
                    fontWeight = FontWeight.Bold,
                    color = if (hasDebt) Color.Red else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierBottomSheet(supplier: Supplier, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(supplier.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(supplier.category, style = MaterialTheme.typography.titleMedium, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            val hasDebt = supplier.pendingBalance > 0
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = if (hasDebt) Color.Red.copy(alpha = 0.05f) else Color.Green.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Deuda Pendiente", color = Color.Gray)
                    Text(
                        text = "$${"%.2f".format(supplier.pendingBalance)}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (hasDebt) Color.Red else Color(0xFF388E3C)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (hasDebt) {
                Button(
                    onClick = { /* Registrar Pago */ },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Icon(Icons.Default.Payment, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Registrar Pago", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                OutlinedButton(
                    onClick = { /* Ver Historial */ },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Ver Historial de Facturas", style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
