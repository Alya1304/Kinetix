package com.yach.kinetix.frontofhouse.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yach.kinetix.frontofhouse.data.entity.CustomerAccountMovementEntity
import com.yach.kinetix.frontofhouse.data.entity.CustomerEntity
import com.yach.kinetix.frontofhouse.data.entity.CustomerMovementType
import java.math.BigDecimal
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    viewModel: CustomersViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val isTabletOrLandscape = configuration.screenWidthDp >= 600

    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showMobileCustomerDetailSheet by remember { mutableStateOf(false) }

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
                title = { Text("Clientes CRM & Cuentas Corrientes", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showAddCustomerDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo Cliente")
            }
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Panel Izquierdo: Buscador y Lista de Clientes
            Column(
                modifier = Modifier
                    .weight(if (isTabletOrLandscape) 0.4f else 1f)
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.search(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar por nombre o teléfono...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.customers, key = { it.id }) { customer ->
                        val isSelected = uiState.selectedCustomer?.id == customer.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.selectCustomer(customer)
                                    if (!isTabletOrLandscape) {
                                        showMobileCustomerDetailSheet = true
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = customer.fullName.take(2).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = customer.fullName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (!customer.phone.isNullOrBlank()) {
                                        Text(
                                            text = customer.phone,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )
                                    }
                                }

                                if (customer.currentDebt > BigDecimal.ZERO) {
                                    Surface(
                                        color = Color(0xFFC62828).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "Debe $${customer.currentDebt}",
                                            color = Color(0xFFC62828),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                } else {
                                    Surface(
                                        color = Color(0xFF2E7D32).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "Al día",
                                            color = Color(0xFF2E7D32),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Panel Derecho (en Tablets/Landscape): Ficha Detallada del Cliente
            if (isTabletOrLandscape) {
                Box(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(20.dp)
                ) {
                    val customer = uiState.selectedCustomer
                    if (customer == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Selecciona un cliente", style = MaterialTheme.typography.titleLarge, color = Color.Gray)
                                Text("Consulta su saldo, límite de crédito e histórico de abonos.", color = Color.Gray)
                            }
                        }
                    } else {
                        CustomerDetailPanel(
                            customer = customer,
                            movements = uiState.movements,
                            onMakePaymentClick = { showPaymentDialog = true }
                        )
                    }
                }
            }
        }
    }

    if (!isTabletOrLandscape && showMobileCustomerDetailSheet && uiState.selectedCustomer != null) {
        ModalBottomSheet(
            onDismissRequest = { showMobileCustomerDetailSheet = false }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(20.dp)
            ) {
                CustomerDetailPanel(
                    customer = uiState.selectedCustomer!!,
                    movements = uiState.movements,
                    onMakePaymentClick = {
                        showPaymentDialog = true
                    }
                )
            }
        }
    }

    if (showAddCustomerDialog) {
        AddCustomerDialog(
            onDismiss = { showAddCustomerDialog = false },
            onSave = { name, taxId, phone, email, address, discount, creditLimit ->
                viewModel.createCustomer(name, taxId, phone, email, address, discount, creditLimit)
                showAddCustomerDialog = false
            }
        )
    }

    if (showPaymentDialog && uiState.selectedCustomer != null) {
        val dummyCashierId = UUID.fromString("00000000-0000-0000-0000-000000000001")
        PaymentAbonoDialog(
            customer = uiState.selectedCustomer!!,
            onDismiss = { showPaymentDialog = false },
            onConfirm = { amount, isCash, notes ->
                viewModel.registerPaymentAbono(uiState.selectedCustomer!!.id, amount, isCash, notes, dummyCashierId)
                showPaymentDialog = false
            }
        )
    }
}

@Composable
fun CustomerDetailPanel(
    customer: CustomerEntity,
    movements: List<CustomerAccountMovementEntity>,
    onMakePaymentClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Cabecera del Cliente
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(customer.fullName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                if (!customer.taxId.isNullOrBlank()) {
                    Text("RFC / Identificación: ${customer.taxId}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
            }
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onMakePaymentClick()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.height(52.dp)
            ) {
                Icon(Icons.Default.Payment, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("REGISTRAR ABONO", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tarjetas de Métricas de Crédito
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = if (customer.currentDebt > BigDecimal.ZERO) Color(0xFFC62828).copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Deuda Actual", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "$${customer.currentDebt}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (customer.currentDebt > BigDecimal.ZERO) Color(0xFFC62828) else Color(0xFF2E7D32)
                    )
                }
            }

            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Límite de Crédito", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$${customer.creditLimit}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
            }

            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Descuento Habitual", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${customer.defaultDiscountRate.multiply(BigDecimal(100)).toInt()}%", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text("Historial de Cuenta Corriente (Abonos y Cargos)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        if (movements.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("No hay movimientos registrados en la cuenta de este cliente.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(movements, key = { it.id }) { mov ->
                    val isAbono = mov.type == CustomerMovementType.PAYMENT_ABONO
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            if (isAbono) Color(0xFF2E7D32).copy(alpha = 0.15f) else Color(0xFFC62828).copy(alpha = 0.15f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isAbono) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        tint = if (isAbono) Color(0xFF2E7D32) else Color(0xFFC62828),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (isAbono) "Abono a Cuenta" else "Consumo a Crédito",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (!mov.notes.isNullOrBlank()) {
                                        Text(text = mov.notes, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                }
                            }
                            Text(
                                text = "${if (isAbono) "-" else "+"} $${mov.amount}",
                                fontWeight = FontWeight.Bold,
                                color = if (isAbono) Color(0xFF2E7D32) else Color(0xFFC62828),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddCustomerDialog(
    onDismiss: () -> Unit,
    onSave: (String, String?, String?, String?, String?, BigDecimal, BigDecimal) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var taxId by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var creditLimit by remember { mutableStateOf("0") }
    var discount by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Nuevo Cliente") },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre Completo *") }, singleLine = true)
                OutlinedTextField(value = taxId, onValueChange = { taxId = it }, label = { Text("RFC / Identificación") }, singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Teléfono") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true)
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Correo Electrónico") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true)
                OutlinedTextField(value = creditLimit, onValueChange = { creditLimit = it }, label = { Text("Límite de Crédito ($)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                OutlinedTextField(value = discount, onValueChange = { discount = it }, label = { Text("Descuento Habitual (%)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val limit = creditLimit.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val disc = (discount.toBigDecimalOrNull() ?: BigDecimal.ZERO).divide(BigDecimal(100))
                    onSave(name, taxId.ifBlank { null }, phone.ifBlank { null }, email.ifBlank { null }, address.ifBlank { null }, disc, limit)
                },
                enabled = name.isNotBlank()
            ) { Text("Guardar Cliente") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun PaymentAbonoDialog(
    customer: CustomerEntity,
    onDismiss: () -> Unit,
    onConfirm: (BigDecimal, Boolean, String?) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var isCash by remember { mutableStateOf(true) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Abono - ${customer.fullName}") },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Deuda actual pendiente: $${customer.currentDebt}", color = Color(0xFFC62828), fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Monto a Abonar ($) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isCash, onCheckedChange = { isCash = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isCash) "Pago en Efectivo (Ingresa a caja)" else "Transferencia / Tarjeta")
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas / Referencia (Opcional)") },
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

