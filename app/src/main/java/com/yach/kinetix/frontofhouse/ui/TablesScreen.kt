package com.yach.kinetix.frontofhouse.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yach.kinetix.frontofhouse.data.entity.RestaurantTableEntity
import com.yach.kinetix.frontofhouse.data.entity.RoomEntity
import com.yach.kinetix.frontofhouse.data.entity.TableStatus
import java.util.UUID
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TablesScreen(
    viewModel: TablesViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    var showAddRoomDialog by remember { mutableStateOf(false) }
    var showAddTableDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Plano de Mesas", fontWeight = FontWeight.Bold) },
                actions = {
                    IconToggleButton(
                        checked = uiState.isHeatmapModeActive,
                        onCheckedChange = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.toggleHeatmapMode() 
                        }
                    ) {
                        Icon(
                            Icons.Default.LocalFireDepartment, 
                            contentDescription = "Mapa de Calor",
                            tint = if (uiState.isHeatmapModeActive) Color.Red else LocalContentColor.current
                        )
                    }
                    IconToggleButton(
                        checked = uiState.isEditModeActive,
                        onCheckedChange = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.toggleEditMode() 
                        }
                    ) {
                        Icon(
                            Icons.Default.Edit, 
                            contentDescription = "Editar Plano",
                            tint = if (uiState.isEditModeActive) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.isEditModeActive) {
                Column(horizontalAlignment = Alignment.End) {
                    if (uiState.rooms.isEmpty()) {
                        ExtendedFloatingActionButton(
                            onClick = { showAddRoomDialog = true },
                            icon = { Icon(Icons.Default.AddHome, "Añadir Zona") },
                            text = { Text("Añadir Zona") }
                        )
                    } else {
                        FloatingActionButton(onClick = { showAddRoomDialog = true }, modifier = Modifier.padding(bottom = 8.dp)) {
                            Icon(Icons.Default.AddHome, contentDescription = "Añadir Zona")
                        }
                        ExtendedFloatingActionButton(
                            onClick = { showAddTableDialog = true },
                            icon = { Icon(Icons.Default.Add, "Añadir Mesa") },
                            text = { Text("Añadir Mesa") },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    }
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
            if (uiState.rooms.isNotEmpty()) {
                ScrollableTabRow(
                    selectedTabIndex = uiState.rooms.indexOfFirst { it.id == uiState.selectedRoomId }.coerceAtLeast(0),
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    uiState.rooms.forEach { room ->
                        Tab(
                            selected = uiState.selectedRoomId == room.id,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.selectRoom(room.id)
                            },
                            text = { Text(room.name) }
                        )
                    }
                }

                // Plano Arquitectónico (Canvas)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.LightGray.copy(alpha = 0.1f)) // Fondo tipo plano
                ) {
                    if (uiState.tables.isEmpty() && !uiState.isEditModeActive) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No hay mesas en esta zona.", color = Color.Gray)
                            Text("Activa el modo edición (✏️) para agregar mesas.", color = Color.Gray)
                        }
                    }

                    uiState.tables.forEach { table ->
                        val heatScore = uiState.mockHeatScores[table.id] ?: 0f
                        
                        var offsetX by remember(table.id) { mutableFloatStateOf(table.positionX) }
                        var offsetY by remember(table.id) { mutableFloatStateOf(table.positionY) }

                        // Si no tienen posición, las organizamos
                        LaunchedEffect(table.id) {
                            if (table.positionX == 0f && table.positionY == 0f) {
                                val idx = uiState.tables.indexOf(table)
                                val baseOffset = 50f
                                val spacing = 450f
                                offsetX = baseOffset + (idx % 3) * spacing
                                offsetY = baseOffset + (idx / 3) * spacing
                                viewModel.updateTablePosition(table.id, offsetX, offsetY)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                                .then(
                                    if (uiState.isEditModeActive) {
                                        Modifier.pointerInput(table.id) {
                                            detectDragGestures(
                                                onDragEnd = {
                                                    viewModel.updateTablePosition(table.id, offsetX, offsetY)
                                                }
                                            ) { change, dragAmount ->
                                                change.consume()
                                                offsetX += dragAmount.x
                                                offsetY += dragAmount.y
                                            }
                                        }
                                    } else Modifier
                                )
                        ) {
                            TableCard(
                                table = table,
                                isHeatmapActive = uiState.isHeatmapModeActive,
                                heatScore = heatScore,
                                isEditModeActive = uiState.isEditModeActive,
                                onClick = {
                                    if (!uiState.isEditModeActive) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.selectTable(table.id)
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("El plano está vacío", style = MaterialTheme.typography.titleLarge)
                        Text("Activa el modo edición (✏️) para comenzar", color = Color.Gray)
                    }
                }
            }
        }
    }

    if (showAddRoomDialog) {
        var roomName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddRoomDialog = false },
            title = { Text("Nueva Zona") },
            text = {
                OutlinedTextField(
                    value = roomName,
                    onValueChange = { roomName = it },
                    label = { Text("Nombre de la zona (ej. Terraza)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createRoom(roomName)
                        showAddRoomDialog = false
                    },
                    enabled = roomName.isNotBlank()
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { showAddRoomDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (showAddTableDialog) {
        var tableNum by remember { mutableStateOf("") }
        var capacity by remember { mutableStateOf("4") }
        AlertDialog(
            onDismissRequest = { showAddTableDialog = false },
            title = { Text("Nueva Mesa") },
            text = {
                Column {
                    OutlinedTextField(
                        value = tableNum,
                        onValueChange = { tableNum = it },
                        label = { Text("Número/Nombre de Mesa") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = capacity,
                        onValueChange = { capacity = it },
                        label = { Text("Capacidad (Personas)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cap = capacity.toIntOrNull() ?: 4
                        viewModel.addTable(tableNum, cap)
                        showAddTableDialog = false
                    },
                    enabled = tableNum.isNotBlank()
                ) { Text("Añadir") }
            },
            dismissButton = {
                TextButton(onClick = { showAddTableDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Modal para Acciones de Mesa
    if (uiState.selectedTableId != null) {
        val selectedTable = uiState.tables.find { it.id == uiState.selectedTableId }
        if (selectedTable != null) {
            TableActionSheet(
                table = selectedTable,
                availableTables = uiState.tables.filter { it.status == TableStatus.AVAILABLE },
                onDismiss = { viewModel.selectTable(null) },
                onOpenOrder = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.changeTableStatus(selectedTable.id, TableStatus.OCCUPIED)
                    viewModel.selectTable(null)
                },
                onTransfer = { targetTableId ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.transferOrder(selectedTable.id, targetTableId, UUID.randomUUID())
                    viewModel.selectTable(null)
                },
                onPrintPreAccount = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.changeTableStatus(selectedTable.id, TableStatus.PAYING)
                    viewModel.selectTable(null)
                },
                onClearTable = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.changeTableStatus(selectedTable.id, TableStatus.AVAILABLE)
                    viewModel.selectTable(null)
                }
            )
        } else {
            viewModel.selectTable(null)
        }
    }
}

@Composable
fun TableCard(
    table: RestaurantTableEntity,
    isHeatmapActive: Boolean,
    heatScore: Float,
    isEditModeActive: Boolean,
    onClick: () -> Unit
) {
    val cardContainerColor: Color
    val cardContentColor: Color
    val cardBorder: Modifier

    if (isHeatmapActive) {
        val red = (heatScore * 255).toInt()
        val blue = ((1 - heatScore) * 255).toInt()
        cardContainerColor = Color(red, 50, blue, 255)
        cardContentColor = Color.White
        cardBorder = Modifier
    } else {
        when (table.status) {
            TableStatus.AVAILABLE -> {
                cardContainerColor = MaterialTheme.colorScheme.surface
                cardContentColor = MaterialTheme.colorScheme.onSurface
                cardBorder = Modifier.border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            }
            TableStatus.OCCUPIED -> {
                cardContainerColor = MaterialTheme.colorScheme.primaryContainer
                cardContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                cardBorder = Modifier
            }
            TableStatus.PAYING -> {
                cardContainerColor = MaterialTheme.colorScheme.secondaryContainer
                cardContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                cardBorder = Modifier
            }
        }
    }

    Card(
        modifier = Modifier
            .size(160.dp)
            .then(cardBorder)
            .clickable(enabled = !isEditModeActive) { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor, contentColor = cardContentColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (table.status != TableStatus.AVAILABLE || isHeatmapActive || isEditModeActive) 6.dp else 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            
            if (isEditModeActive) {
                Icon(
                    Icons.Default.DragIndicator, 
                    contentDescription = "Mover", 
                    modifier = Modifier.align(Alignment.TopEnd).size(20.dp),
                    tint = Color.Gray
                )
            } else {
                Row(
                    modifier = Modifier.align(Alignment.TopEnd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Person, contentDescription = "Capacidad", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("${table.capacity}", style = MaterialTheme.typography.labelSmall)
                }
            }

            if (!table.tags.isNullOrBlank() && !isEditModeActive) {
                val tagsList = table.tags.split(",")
                Row(
                    modifier = Modifier.align(Alignment.TopStart),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tagsList.take(2).forEach { tag ->
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.9f), CircleShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = tag.trim(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Black
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Mesa ${table.tableNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (isHeatmapActive) {
                    Text(
                        text = "Score: ${(heatScore * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium
                    )
                } else if (!isEditModeActive) {
                    when (table.status) {
                        TableStatus.AVAILABLE -> Text("Libre", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                        TableStatus.OCCUPIED -> {
                            Text("Ocupada", style = MaterialTheme.typography.labelMedium)
                            if (table.currentOrderId != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("45 min", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        TableStatus.PAYING -> Text("Pagando", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (table.status == TableStatus.OCCUPIED && !isHeatmapActive && !isEditModeActive) {
                Text(
                    text = "$1,250.00",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableActionSheet(
    table: RestaurantTableEntity,
    availableTables: List<RestaurantTableEntity>,
    onDismiss: () -> Unit,
    onOpenOrder: () -> Unit,
    onTransfer: (UUID) -> Unit,
    onPrintPreAccount: () -> Unit,
    onClearTable: () -> Unit
) {
    var isTransferMode by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp, start = 24.dp, end = 24.dp, top = 8.dp)
        ) {
            Text(
                text = "Mesa ${table.tableNumber}",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (isTransferMode) {
                Text("Selecciona la mesa destino:", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(100.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 200.dp)
                ) {
                    items(availableTables, key = { it.id }) { target ->
                        Button(
                            onClick = { onTransfer(target.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                        ) {
                            Text(target.tableNumber)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(onClick = { isTransferMode = false }, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancelar Traslado")
                }
            } else {
                when (table.status) {
                    TableStatus.AVAILABLE -> {
                        Button(
                            onClick = onOpenOrder,
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Icon(Icons.Default.RestaurantMenu, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Abrir Comanda", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    TableStatus.OCCUPIED -> {
                        Button(
                            onClick = { onDismiss() },
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ir a Comanda", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { isTransferMode = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Trasladar Mesa")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onPrintPreAccount,
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Icon(Icons.Default.Receipt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Imprimir Pre-cuenta")
                        }
                    }
                    TableStatus.PAYING -> {
                        Button(
                            onClick = onClearTable,
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cobrar y Liberar", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}
