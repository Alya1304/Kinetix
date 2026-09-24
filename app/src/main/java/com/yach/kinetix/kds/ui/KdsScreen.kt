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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yach.kinetix.backofhouse.ui.KdsViewModel
import com.yach.kinetix.backofhouse.ui.KdsOrderCardUiModel
import com.yach.kinetix.backofhouse.ui.KdsSlaStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KdsScreen(
    viewModel: KdsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val stations = listOf("Todas", "Parrilla", "Mesa Fría", "Bebidas", "Postres")
    var selectedStationIndex by remember { mutableIntStateOf(0) }

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
                selectedTabIndex = selectedStationIndex,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                stations.forEachIndexed { index, stationName ->
                    Tab(
                        selected = selectedStationIndex == index,
                        onClick = { selectedStationIndex = index },
                        text = { 
                            Text(
                                text = stationName,
                                fontWeight = if (selectedStationIndex == index) FontWeight.Bold else FontWeight.Normal
                            ) 
                        }
                    )
                }
            }

            // Tablero Kanban de Tickets
            if (uiState.orderCards.isEmpty()) {
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
                    items(uiState.orderCards, key = { it.orderDetails.order.id }) { order ->
                        KdsTicketCard(order = order)
                    }
                }
            }
        }
    }
}

@Composable
fun KdsTicketCard(order: KdsOrderCardUiModel) {
    // Determinar el color superior (Header) segun la prioridad (SLA)
    val headerColor = when (order.slaStatus) {
        KdsSlaStatus.VERY_DELAYED -> Color(0xFFD32F2F) // Rojo
        KdsSlaStatus.DELAYED -> Color(0xFFF57C00) // Naranja/Amarillo oscuro
        KdsSlaStatus.ON_TIME -> Color(0xFF1976D2) // Azul
    }

    Card(
        modifier = Modifier
            .width(320.dp)
            .fillMaxHeight(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
    ) {
        Column {
            // Header del Ticket
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerColor)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Orden #${order.orderDetails.order.id.toString().take(4)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Para Llevar", // Mock for now
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                
                Surface(
                    color = Color.Black.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${order.minutesElapsed} min",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Cuerpo del Ticket (Items)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .weight(1f)
            ) {
                order.orderDetails.items.forEach { item ->
                    val isCompleted = false
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable { /* Toggle completion status */ },
                        verticalAlignment = Alignment.Top
                    ) {
                        // Checkbox custom
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(24.dp)
                                .background(
                                    color = if (isCompleted) Color(0xFF4CAF50) else Color.Transparent,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .background(
                                    color = if (isCompleted) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(4.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompleted) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${item.item.quantity}x",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isCompleted) Color.Gray else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = item.item.productName,
                                    style = MaterialTheme.typography.titleMedium,
                                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                    color = if (isCompleted) Color.Gray else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            if (!item.item.itemNotes.isNullOrEmpty()) {
                                Row(modifier = Modifier.padding(top = 4.dp)) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF57C00), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = item.item.itemNotes,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontStyle = FontStyle.Italic,
                                        color = Color(0xFFF57C00)
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                }
            }

            // Acciones del Ticket
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = { /* Despachar */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Marcar Listo", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
