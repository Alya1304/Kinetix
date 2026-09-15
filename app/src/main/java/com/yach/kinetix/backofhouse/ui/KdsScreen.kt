package com.yach.kinetix.backofhouse.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.OutdoorGrill
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yach.kinetix.frontofhouse.data.entity.KdsItemStatus
import com.yach.kinetix.frontofhouse.data.entity.OrderStatus
import com.yach.kinetix.frontofhouse.data.entity.OrderType
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KdsScreen(
    viewModel: KdsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dummyCookId = UUID.fromString("00000000-0000-0000-0000-000000000001")

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Monitor de Cocina (KDS)", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(16.dp))
                        Badge(
                            containerColor = if (uiState.orderCards.isNotEmpty()) MaterialTheme.colorScheme.primary else Color.Gray,
                            contentColor = Color.White
                        ) {
                            Text(
                                text = "${uiState.orderCards.size} comandas",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.orderCards.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CheckCircleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "¡Cocina Limpia y al Día!",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No hay órdenes pendientes de preparación en este momento.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val isTabletOrLandscape = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 600
                if (isTabletOrLandscape) {
                    LazyRow(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(uiState.orderCards, key = { it.orderDetails.order.id }) { card ->
                            KdsTicketCard(
                                card = card,
                                modifier = Modifier
                                    .width(340.dp)
                                    .fillMaxHeight(),
                                onItemClick = { orderItemId ->
                                    viewModel.setItemReady(orderItemId, dummyCookId)
                                },
                                onStartCooking = {
                                    viewModel.setOrderCooking(card.orderDetails.order.id, dummyCookId)
                                },
                                onCompleteOrder = {
                                    viewModel.setOrderReady(card.orderDetails.order.id, dummyCookId)
                                }
                            )
                        }
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(uiState.orderCards, key = { it.orderDetails.order.id }) { card ->
                            KdsTicketCard(
                                card = card,
                                modifier = Modifier.fillMaxWidth(),
                                onItemClick = { orderItemId ->
                                    viewModel.setItemReady(orderItemId, dummyCookId)
                                },
                                onStartCooking = {
                                    viewModel.setOrderCooking(card.orderDetails.order.id, dummyCookId)
                                },
                                onCompleteOrder = {
                                    viewModel.setOrderReady(card.orderDetails.order.id, dummyCookId)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KdsTicketCard(
    card: KdsOrderCardUiModel,
    modifier: Modifier = Modifier,
    onItemClick: (UUID) -> Unit,
    onStartCooking: () -> Unit,
    onCompleteOrder: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val order = card.orderDetails.order
    val items = card.orderDetails.items

    val (headerColor, headerTextColor) = when (card.slaStatus) {
        KdsSlaStatus.ON_TIME -> Color(0xFF2E7D32) to Color.White       // Verde
        KdsSlaStatus.DELAYED -> Color(0xFFF57F17) to Color.Black      // Amarillo
        KdsSlaStatus.VERY_DELAYED -> Color(0xFFC62828) to Color.White // Rojo crítico
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Cabecera del Ticket con Semáforo SLA
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
                        text = "Orden #${order.orderNumber}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = headerTextColor
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            tint = headerTextColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${card.minutesElapsed} min",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = headerTextColor
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val typeLabel = when (order.orderType) {
                        OrderType.DINE_IN -> "Comedor"
                        OrderType.TAKEAWAY -> "Para Llevar"
                        OrderType.DELIVERY_APP -> "Delivery (${order.deliveryPlatform ?: "App"})"
                    }
                    Text(text = typeLabel, color = headerTextColor, style = MaterialTheme.typography.bodyMedium)
                    val statusText = when (order.status) {
                        OrderStatus.PENDING -> "Pendiente"
                        OrderStatus.IN_PREPARATION -> "En Cocina 👨‍🍳"
                        OrderStatus.READY -> "Listo ✅"
                        else -> ""
                    }
                    Text(text = statusText, color = headerTextColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Lista de platillos
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(12.dp)
            ) {
                items.forEach { itemWithModifiers ->
                    val item = itemWithModifiers.item
                    val isItemDone = item.kdsStatus == KdsItemStatus.READY

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onItemClick(item.id)
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isItemDone,
                            onCheckedChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onItemClick(item.id)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${item.quantity}x",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    textDecoration = if (isItemDone) TextDecoration.LineThrough else null,
                                    color = if (isItemDone) Color.Gray else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = item.productName,
                                    style = MaterialTheme.typography.titleMedium,
                                    textDecoration = if (isItemDone) TextDecoration.LineThrough else null,
                                    color = if (isItemDone) Color.Gray else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (!item.itemNotes.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Nota: ${item.itemNotes}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFD32F2F),
                                    fontStyle = FontStyle.Italic
                                )
                            }
                            itemWithModifiers.modifiers.forEach { mod ->
                                Text(
                                    text = "+ ${mod.modifierName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    fontStyle = FontStyle.Italic
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }

            // Pie de tarjeta / Botón de acción
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(16.dp)
            ) {
                if (order.status == OrderStatus.PENDING) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onStartCooking()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.OutdoorGrill, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("INICIAR PREPARACIÓN", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onCompleteOrder()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ORDEN LISTA (Despachar)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

