package com.yach.kinetix.dashboard.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToPos: () -> Unit = {},
    onNavigateToTables: () -> Unit = {},
    onNavigateToKds: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val configuration = LocalConfiguration.current
    val isMobile = configuration.screenWidthDp < 600

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Encabezado
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Buen Día, Administrador",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                val currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd 'de' MMMM, yyyy", Locale("es", "MX")))
                Text(
                    text = "Turno actual • $currentDate",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
            Text(
                text = "Sincronizado en vivo",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // KPIs
        if (isMobile) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    KpiCard(
                        title = "Ventas del Día",
                        value = formatCurrency(uiState.grossSales.toDouble()),
                        icon = Icons.Default.AttachMoney,
                        microcopy = "${uiState.totalOrders} pedidos",
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Órdenes",
                        value = uiState.totalOrders.toString(),
                        icon = Icons.Default.Receipt,
                        microcopy = "Procesadas",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    KpiCard(
                        title = "Ticket Promedio",
                        value = formatCurrency(uiState.averageTicket.toDouble()),
                        icon = Icons.Default.ConfirmationNumber,
                        microcopy = "Por orden",
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Mesas Activas",
                        value = uiState.activeTables.toString(),
                        icon = Icons.Default.TableRestaurant,
                        microcopy = "En servicio",
                        statusColor = if (uiState.activeTables > 0) Color(0xFFE53935) else Color.Gray,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToTables
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                KpiCard(
                    title = "Ventas del Día",
                    value = formatCurrency(uiState.grossSales.toDouble()),
                    icon = Icons.Default.AttachMoney,
                    microcopy = "${uiState.totalOrders} pedidos",
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Órdenes",
                    value = uiState.totalOrders.toString(),
                    icon = Icons.Default.Receipt,
                    microcopy = "Procesadas",
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Ticket Promedio",
                    value = formatCurrency(uiState.averageTicket.toDouble()),
                    icon = Icons.Default.ConfirmationNumber,
                    microcopy = "Por orden",
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Mesas Activas",
                    value = uiState.activeTables.toString(),
                    icon = Icons.Default.TableRestaurant,
                    microcopy = "En servicio",
                    statusColor = if (uiState.activeTables > 0) Color(0xFFE53935) else Color.Gray,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToTables
                )
            }
        }

        // Accesos Rápidos
        Column {
            Text("Accesos Rápidos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                QuickActionCard(title = "Nueva Venta", icon = Icons.Default.AddShoppingCart, onClick = onNavigateToPos, modifier = Modifier.weight(1f))
                QuickActionCard(title = "Monitor KDS", icon = Icons.Default.Restaurant, onClick = onNavigateToKds, modifier = Modifier.weight(1f))
                QuickActionCard(title = "Plano Mesas", icon = Icons.Default.TableBar, onClick = onNavigateToTables, modifier = Modifier.weight(1f))
            }
        }

        // Charts and Lists Row
        if (isMobile) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                        Text("Evolución de Ventas (Hoy)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        RevenueChart(modifier = Modifier.weight(1f).fillMaxWidth())
                    }
                }

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                        Text("Top 5 Platillos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (uiState.topSellingItems.isEmpty()) {
                            Text("No hay datos suficientes", color = Color.Gray, modifier = Modifier.padding(top = 16.dp))
                        } else {
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
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().height(350.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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

                ElevatedCard(
                    modifier = Modifier.weight(0.4f).fillMaxHeight(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                        Text("Top 5 Platillos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (uiState.topSellingItems.isEmpty()) {
                            Text("No hay datos suficientes", color = Color.Gray, modifier = Modifier.padding(top = 16.dp))
                        } else {
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
            }
        }

        // Bottom Stats Row
        val bottomStatsContent = @Composable { modifier: Modifier ->
            Row(
                modifier = modifier,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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

                ElevatedCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Ingresos por Pago", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        PaymentMethodRow("Efectivo", uiState.cashRevenue.toDouble(), Icons.Default.Money)
                        PaymentMethodRow("Tarjeta", uiState.cardRevenue.toDouble(), Icons.Default.CreditCard)
                        PaymentMethodRow("Transf.", uiState.transferRevenue.toDouble(), Icons.Default.PhoneIphone)
                    }
                }
            }
        }

        if (isMobile) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Ocupación de Mesas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            OccupancyStat(label = "Activas", count = uiState.activeTables, color = Color(0xFFE53935))
                            OccupancyStat(label = "Libres", count = uiState.freeTables, color = Color(0xFF43A047))
                        }
                    }
                }
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Ingresos por Pago", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        PaymentMethodRow("Efectivo", uiState.cashRevenue.toDouble(), Icons.Default.Money)
                        PaymentMethodRow("Tarjeta", uiState.cardRevenue.toDouble(), Icons.Default.CreditCard)
                        PaymentMethodRow("Transferencia", uiState.transferRevenue.toDouble(), Icons.Default.PhoneIphone)
                    }
                }
            }
        } else {
            bottomStatsContent(Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    icon: ImageVector,
    microcopy: String? = null,
    statusColor: Color? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    ElevatedCard(
        modifier = if (onClick != null) modifier.clickable { onClick() } else modifier,
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            if (statusColor != null) {
                Box(modifier = Modifier.align(Alignment.TopEnd).size(8.dp).background(statusColor, CircleShape))
            }
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                if (microcopy != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = microcopy, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionCard(title: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
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
fun PaymentMethodRow(label: String, amount: Double, icon: ImageVector) {
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
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
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
