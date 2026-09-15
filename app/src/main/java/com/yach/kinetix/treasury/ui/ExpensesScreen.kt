package com.yach.kinetix.treasury.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Phone
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yach.kinetix.frontofhouse.data.entity.PaymentMethod
import com.yach.kinetix.treasury.data.entity.ExpenseCategoryEntity
import com.yach.kinetix.treasury.data.entity.ExpenseEntity
import com.yach.kinetix.treasury.data.entity.ExpenseStatus
import com.yach.kinetix.treasury.data.entity.SupplierEntity
import java.math.BigDecimal
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    viewModel: ExpensesViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Gastos, 1 = Proveedores
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showAddSupplierDialog by remember { mutableStateOf(false) }
    var selectedSupplierForPayment by remember { mutableStateOf<SupplierEntity?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Gastos Operativos & Proveedores", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (selectedTab == 0) showAddExpenseDialog = true else showAddSupplierDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = if (selectedTab == 0) "Nuevo Gasto" else "Nuevo Proveedor")
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
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Gastos de Caja", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Proveedores / Pasivos", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                )
            }

            if (selectedTab == 0) {
                ExpensesTabContent(
                    expenses = uiState.expenses,
                    categories = uiState.categories,
                    suppliers = uiState.suppliers
                )
            } else {
                SuppliersTabContent(
                    suppliers = uiState.suppliers,
                    onPaySupplier = { supplier ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedSupplierForPayment = supplier
                    }
                )
            }
        }
    }

    if (showAddExpenseDialog) {
        val dummyUserId = UUID.fromString("00000000-0000-0000-0000-000000000001")
        AddExpenseDialog(
            categories = uiState.categories,
            suppliers = uiState.suppliers,
            onDismiss = { showAddExpenseDialog = false },
            onSave = { categoryId, supplierId, amount, isPaid, notes ->
                viewModel.registerExpense(
                    categoryId = categoryId,
                    supplierId = supplierId,
                    amount = amount,
                    dueDate = null,
                    paymentMethod = if (isPaid) PaymentMethod.CASH else null,
                    status = if (isPaid) ExpenseStatus.PAID else ExpenseStatus.PENDING,
                    notes = notes,
                    userId = dummyUserId
                )
                showAddExpenseDialog = false
            }
        )
    }

    if (showAddSupplierDialog) {
        AddSupplierDialog(
            onDismiss = { showAddSupplierDialog = false },
            onSave = { name, contact, phone, category ->
                viewModel.createSupplier(name, contact, phone, category)
                showAddSupplierDialog = false
            }
        )
    }

    if (selectedSupplierForPayment != null) {
        val dummyUserId = UUID.fromString("00000000-0000-0000-0000-000000000001")
        SupplierPaymentDialog(
            supplier = selectedSupplierForPayment!!,
            onDismiss = { selectedSupplierForPayment = null },
            onConfirm = { amount, isCash, notes ->
                viewModel.registerSupplierPayment(
                    supplierId = selectedSupplierForPayment!!.id,
                    amount = amount,
                    isCash = isCash,
                    notes = notes,
                    userId = dummyUserId
                )
                selectedSupplierForPayment = null
            }
        )
    }
}

@Composable
fun ExpensesTabContent(
    expenses: List<ExpenseEntity>,
    categories: List<ExpenseCategoryEntity>,
    suppliers: List<SupplierEntity>
) {
    val totalPaid = expenses.filter { it.status == ExpenseStatus.PAID }.fold(BigDecimal.ZERO) { acc, exp -> acc.add(exp.amount) }
    val totalPending = expenses.filter { it.status == ExpenseStatus.PENDING }.fold(BigDecimal.ZERO) { acc, exp -> acc.add(exp.amount) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Tarjetas KPI
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Gastos Pagados (Caja)", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$$totalPaid", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Cuentas Pendientes de Pago", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$$totalPending", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Histórico de Gastos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        if (expenses.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay gastos registrados en el turno.", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(expenses, key = { it.id }) { expense ->
                    val catName = categories.find { it.id == expense.categoryId }?.name ?: "Gasto General"
                    val supName = suppliers.find { it.id == expense.supplierId }?.name
                    val isPaid = expense.status == ExpenseStatus.PAID

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            if (isPaid) Color(0xFF2E7D32).copy(alpha = 0.15f) else Color(0xFFF57F17).copy(alpha = 0.15f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPaid) Icons.Default.CheckCircle else Icons.Default.Pending,
                                        contentDescription = null,
                                        tint = if (isPaid) Color(0xFF2E7D32) else Color(0xFFF57F17),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(catName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    if (supName != null) {
                                        Text("Proveedor: $supName", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                    if (!expense.notes.isNullOrBlank()) {
                                        Text("Nota: ${expense.notes}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "$${expense.amount}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = if (isPaid) MaterialTheme.colorScheme.onSurface else Color(0xFFC62828)
                                )
                                Text(
                                    text = if (isPaid) "Pagado en Efectivo" else "Pendiente de Pago",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isPaid) Color(0xFF2E7D32) else Color(0xFFF57F17),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SuppliersTabContent(
    suppliers: List<SupplierEntity>,
    onPaySupplier: (SupplierEntity) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Catálogo de Proveedores y Pasivos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        if (suppliers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay proveedores registrados.", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(suppliers, key = { it.id }) { supplier ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(supplier.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                if (!supplier.contactName.isNullOrBlank()) {
                                    Text("Contacto: ${supplier.contactName}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                if (!supplier.phone.isNullOrBlank()) {
                                    Text("Tel: ${supplier.phone}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Deuda: $${supplier.currentDebt}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (supplier.currentDebt > BigDecimal.ZERO) Color(0xFFC62828) else Color(0xFF2E7D32)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = { onPaySupplier(supplier) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Abonar", style = MaterialTheme.typography.labelMedium)
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
fun AddExpenseDialog(
    categories: List<ExpenseCategoryEntity>,
    suppliers: List<SupplierEntity>,
    onDismiss: () -> Unit,
    onSave: (UUID, UUID?, BigDecimal, Boolean, String?) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf(categories.firstOrNull()?.id) }
    var selectedSupplierId by remember { mutableStateOf<UUID?>(null) }
    var isPaidNow by remember { mutableStateOf(true) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Gasto Operativo") },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Monto del Gasto ($) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Categoría del Gasto:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories, key = { it.id }) { cat ->
                        FilterChip(
                            selected = selectedCategoryId == cat.id,
                            onClick = { selectedCategoryId = cat.id },
                            label = { Text(cat.name) }
                        )
                    }
                }

                if (suppliers.isNotEmpty()) {
                    Text("Proveedor (Opcional):", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(suppliers, key = { it.id }) { sup ->
                            FilterChip(
                                selected = selectedSupplierId == sup.id,
                                onClick = {
                                    selectedSupplierId = if (selectedSupplierId == sup.id) null else sup.id
                                },
                                label = { Text(sup.name) }
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isPaidNow, onCheckedChange = { isPaidNow = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isPaidNow) "Pagado en efectivo (Descontar de caja)" else "Pendiente de pago (Cuentas por pagar)")
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Justificación / Notas") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val catId = selectedCategoryId ?: return@Button
                    if (amount > BigDecimal.ZERO) {
                        onSave(catId, selectedSupplierId, amount, isPaidNow, notes.ifBlank { null })
                    }
                },
                enabled = (amountText.toBigDecimalOrNull() ?: BigDecimal.ZERO) > BigDecimal.ZERO && selectedCategoryId != null
            ) { Text("Guardar Gasto") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun AddSupplierDialog(
    onDismiss: () -> Unit,
    onSave: (String, String?, String?, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Nuevo Proveedor") },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre / Razón Social *") }, singleLine = true)
                OutlinedTextField(value = contact, onValueChange = { contact = it }, label = { Text("Nombre de Contacto") }, singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Teléfono") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true)
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Giro / Categoría (ej. Cárnicos)") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, contact.ifBlank { null }, phone.ifBlank { null }, category.ifBlank { null }) },
                enabled = name.isNotBlank()
            ) { Text("Guardar Proveedor") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun SupplierPaymentDialog(
    supplier: SupplierEntity,
    onDismiss: () -> Unit,
    onConfirm: (BigDecimal, Boolean, String?) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var isCash by remember { mutableStateOf(true) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Abonar a Proveedor - ${supplier.name}") },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Deuda pendiente con proveedor: $${supplier.currentDebt}", color = Color(0xFFC62828), fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Monto del Abono ($) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isCash, onCheckedChange = { isCash = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isCash) "Efectivo (Afecta caja)" else "Transferencia")
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas / Referencia de Pago") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    if (amount > BigDecimal.ZERO) {
                        onConfirm(amount, isCash, notes.ifBlank { null })
                    }
                },
                enabled = (amountText.toBigDecimalOrNull() ?: BigDecimal.ZERO) > BigDecimal.ZERO
            ) { Text("Confirmar Abono") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

