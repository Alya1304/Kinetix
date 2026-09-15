package com.yach.kinetix.crm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrmScreen(
    viewModel: CrmViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Acción para agregar cliente */ },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Agregar Cliente")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Buscador
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                onSearch = { viewModel.onSearchActiveChange(false) },
                active = uiState.isSearchActive,
                onActiveChange = viewModel::onSearchActiveChange,
                placeholder = { Text("Buscar por nombre o teléfono...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Resultados de búsqueda activos podrían ir aquí si quisieramos pantalla completa
            }

            // Pestañas de Filtro
            TabRow(
                selectedTabIndex = uiState.selectedFilterIndex,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                uiState.filters.forEachIndexed { index, title ->
                    Tab(
                        selected = uiState.selectedFilterIndex == index,
                        onClick = { viewModel.selectFilter(index) },
                        text = { Text(title, fontWeight = if (uiState.selectedFilterIndex == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            // Lista de Clientes
            if (uiState.customers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.GroupOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No se encontraron clientes", style = MaterialTheme.typography.titleLarge)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 300.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.customers, key = { it.id }) { customer ->
                        CustomerCard(
                            customer = customer,
                            onClick = { viewModel.selectCustomer(customer.id) }
                        )
                    }
                }
            }
        }
    }

    // Modal Profile
    if (uiState.selectedCustomerId != null) {
        val customer = uiState.customers.find { it.id == uiState.selectedCustomerId }
        if (customer != null) {
            CustomerProfileSheet(
                customer = customer,
                onDismiss = { viewModel.selectCustomer(null) }
            )
        } else {
            viewModel.selectCustomer(null)
        }
    }
}

@Composable
fun CustomerCard(customer: Customer, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = customer.name.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            
            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(customer.phone, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
            }

            // Etiqueta VIP
            if (customer.category == CustomerCategory.VIP) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = "VIP",
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(24.dp)
                )
            } else if (customer.category == CustomerCategory.DEBTOR) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "Deudor",
                    tint = Color.Red,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerProfileSheet(
    customer: Customer,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = customer.name.take(1).uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (customer.category == CustomerCategory.VIP) {
                            Text("⭐ Cliente VIP", color = Color(0xFFD4AF37), fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                Row {
                    IconButton(onClick = { /* Llama */ }, modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)) {
                        Icon(Icons.Default.Phone, contentDescription = "Llamar", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { /* Editar */ }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatBox(label = "Visitas", value = "${customer.totalVisits}")
                StatBox(label = "Ticket Prom.", value = "$${customer.averageTicket}")
                StatBox(label = "Puntos", value = "${customer.loyaltyPoints}", color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Historial Reciente", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            
            // Lista simulada de últimas visitas
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(customer.lastVisit, fontWeight = FontWeight.Bold)
                        Text("$1,200.00", fontWeight = FontWeight.Bold)
                    }
                    Text("Mesa 4 • Atendido por Carlos R.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Hace 2 meses", fontWeight = FontWeight.Bold)
                        Text("$850.00", fontWeight = FontWeight.Bold)
                    }
                    Text("Para Llevar • Atendido por Mostrador", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun StatBox(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
    }
}
