package com.yach.kinetix.dashboard.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Resumen del Día",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // KPIs Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            KpiCard(
                title = "Ventas del Día",
                value = formatCurrency(uiState.grossSales.toDouble()),
                icon = Icons.Default.AttachMoney,
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                title = "Órdenes",
                value = uiState.totalOrders.toString(),
                icon = Icons.Default.Receipt,
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                title = "Ticket Promedio",
                value = formatCurrency(uiState.averageTicket.toDouble()),
                icon = Icons.Default.ConfirmationNumber,
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                title = "Mesas Activas",
                value = uiState.activeTables.toString(),
                icon = Icons.Default.TableRestaurant,
                modifier = Modifier.weight(1f)
            )
        }

        // Charts and Lists Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Chart
            ElevatedCard(
                modifier = Modifier.weight(0.6f).fillMaxHeight(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                    Text("Evolución de Ventas (Hoy)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    RevenueChart(modifier = Modifier.weight(1f).fillMaxWidth())
                }
            }

            // Top Items
            ElevatedCard(
                modifier = Modifier.weight(0.4f).fillMaxHeight(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                    Text("Top 5 Platillos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(uiState.topSellingItems) { index, item ->
                            TopItemRow(index = index + 1, item = item)
                            if (index < uiState.topSellingItems.lastIndex) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                }
            }
        }

        // Bottom Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Occupancy
            ElevatedCard(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Ocupación de Mesas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        OccupancyStat(label = "Activas", count = uiState.activeTables, color = Color(0xFFE53935))
                        OccupancyStat(label = "Libres", count = uiState.freeTables, color = Color(0xFF43A047))
                    }
                }
            }

            // Payment Methods
            ElevatedCard(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Ingresos por Método de Pago", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    PaymentMethodRow("Efectivo", uiState.cashRevenue.toDouble(), Icons.Default.Money)
                    PaymentMethodRow("Tarjeta", uiState.cardRevenue.toDouble(), Icons.Default.CreditCard)
                    PaymentMethodRow("Transferencia", uiState.transferRevenue.toDouble(), Icons.Default.PhoneIphone)
                }
            }
        }
    }
}

@Composable
fun KpiCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TopItemRow(index: Int, item: TopItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "$index.", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = item.name, style = MaterialTheme.typography.bodyMedium)
        }
        Text(text = "${item.quantity} un.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun OccupancyStat(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = count.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun PaymentMethodRow(label: String, amount: Double, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.Gray)
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
        Text(formatCurrency(amount), fontWeight = FontWeight.Bold)
    }
}

@Composable
fun RevenueChart(modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val path = Path()

        // Mock data points
        val points = listOf(
            0.1f to 0.8f,
            0.2f to 0.6f,
            0.3f to 0.9f,
            0.4f to 0.4f,
            0.5f to 0.7f,
            0.6f to 0.3f,
            0.7f to 0.5f,
            0.8f to 0.2f,
            0.9f to 0.6f,
            1.0f to 0.8f
        )

        path.moveTo(0f, height * points.first().second)
        points.forEach { (x, y) ->
            path.lineTo(width * x, height * y)
        }

        drawPath(
            path = path,
            color = primaryColor,
            style = Stroke(width = 4f)
        )
        
        // Draw some grid lines
        for (i in 1..4) {
            drawLine(
                color = Color.LightGray.copy(alpha = 0.5f),
                start = Offset(0f, height * (i / 5f)),
                end = Offset(width, height * (i / 5f)),
                strokeWidth = 1f
            )
        }
    }
}

private fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
    return format.format(amount)
}
