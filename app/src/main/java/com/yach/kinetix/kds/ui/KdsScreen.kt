package com.yach.kinetix.kds.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KdsScreen(
    viewModel: KdsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Kitchen Display System", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Barra de Estaciones (Filtros)
            ScrollableTabRow(
                selectedTabIndex = uiState.selectedStationIndex,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                uiState.stations.forEachIndexed { index, stationName ->
                    Tab(
                        selected = uiState.selectedStationIndex == index,
                        onClick = { viewModel.selectStation(index) },
                        text = { 
                            Text(
                                text = stationName,
                                fontWeight = if (uiState.selectedStationIndex == index) FontWeight.Bold else FontWeight.Normal
                            ) 
                        }
                    )
                }
            }

            // Tablero Kanban de Tickets
            if (uiState.activeTickets.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircleOutline, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No hay comandas activas", style = MaterialTheme.typography.titleLarge)
                        Text("¡Buen trabajo, cocina limpia!", color = Color.Gray)
                    }
                }
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.activeTickets, key = { it.id }) { ticket ->
                        TicketCard(
                            ticket = ticket,
                            onItemToggle = { itemId -> viewModel.toggleItemCompletion(ticket.id, itemId) },
                            onComplete = { viewModel.completeTicket(ticket.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TicketCard(
    ticket: KdsTicket,
    onItemToggle: (java.util.UUID) -> Unit,
    onComplete: () -> Unit
) {
    // Definir colores basados en urgencia
    val headerColor = when (ticket.urgency) {
        TicketUrgency.NORMAL -> Color(0xFF388E3C) // Verde
        TicketUrgency.WARNING -> Color(0xFFFBC02D) // Amarillo
        TicketUrgency.CRITICAL -> Color(0xFFD32F2F) // Rojo
    }
    
    val headerContentColor = when (ticket.urgency) {
        TicketUrgency.WARNING -> Color.Black
        else -> Color.White
    }

    Card(
        modifier = Modifier
            .width(320.dp)
            .fillMaxHeight(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Cabecera del Ticket
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerColor)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = ticket.orderNumber,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = headerContentColor
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = headerContentColor, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${ticket.elapsedMinutes} min",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = headerContentColor
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(ticket.tableName, color = headerContentColor, style = MaterialTheme.typography.bodyMedium)
                    Text(ticket.waiterName, color = headerContentColor, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Lista de Platillos (Checklist)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(8.dp)
            ) {
                ticket.items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onItemToggle(item.id) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Checkbox(
                            checked = item.isCompleted,
                            onCheckedChange = { onItemToggle(item.id) },
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row {
                                Text(
                                    text = "${item.quantity}x",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null,
                                    color = if (item.isCompleted) Color.Gray else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null,
                                    color = if (item.isCompleted) Color.Gray else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (item.modifiers.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                item.modifiers.forEach { mod ->
                                    Text(
                                        text = "- $mod",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (item.isCompleted) Color.Gray else Color.Red.copy(alpha = 0.8f),
                                        fontStyle = FontStyle.Italic,
                                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null,
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }

            // Footer / Acciones
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(16.dp)
            ) {
                val allCompleted = ticket.items.all { it.isCompleted }
                Button(
                    onClick = onComplete,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (allCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Default.DoneAll, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (allCompleted) "ORDEN LISTA (Despachar)" else "Forzar Despacho",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}
