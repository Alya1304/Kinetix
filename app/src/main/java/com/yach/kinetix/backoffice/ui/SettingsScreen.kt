package com.yach.kinetix.backoffice.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yach.kinetix.core.hardware.PrinterConnectionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    if (uiState.testResultMessage != null) {
        AlertDialog(
            onDismissRequest = viewModel::clearMessage,
            confirmButton = {
                TextButton(onClick = viewModel::clearMessage) { Text("OK") }
            },
            title = { Text("Prueba de Impresora") },
            text = { Text(uiState.testResultMessage ?: "") }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes del Sistema") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Sección de Restaurante
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Store, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Datos del Restaurante", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = uiState.restaurantName,
                        onValueChange = viewModel::updateRestaurantName,
                        label = { Text("Nombre del Restaurante") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = uiState.ticketHeader,
                        onValueChange = viewModel::updateTicketHeader,
                        label = { Text("Mensaje de Cabecera (Ticket)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Sección de Hardware
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Print, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Configuración de Impresora", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Interfaz de Conexión", style = MaterialTheme.typography.bodyMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = uiState.connectionType == PrinterConnectionType.BLUETOOTH,
                            onClick = { viewModel.updateConnectionType(PrinterConnectionType.BLUETOOTH) },
                            label = { Text("Bluetooth") },
                            leadingIcon = { Icon(Icons.Default.Bluetooth, contentDescription = null) }
                        )
                        FilterChip(
                            selected = uiState.connectionType == PrinterConnectionType.TCP_IP,
                            onClick = { viewModel.updateConnectionType(PrinterConnectionType.TCP_IP) },
                            label = { Text("Wi-Fi / Red") },
                            leadingIcon = { Icon(Icons.Default.Wifi, contentDescription = null) }
                        )
                        FilterChip(
                            selected = uiState.connectionType == PrinterConnectionType.USB,
                            onClick = { viewModel.updateConnectionType(PrinterConnectionType.USB) },
                            label = { Text("USB") },
                            leadingIcon = { Icon(Icons.Default.Usb, contentDescription = null) }
                        )
                        FilterChip(
                            selected = uiState.connectionType == PrinterConnectionType.NONE,
                            onClick = { viewModel.updateConnectionType(PrinterConnectionType.NONE) },
                            label = { Text("Ninguna") }
                        )
                    }

                    if (uiState.connectionType != PrinterConnectionType.NONE) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = uiState.printerAddress,
                            onValueChange = viewModel::updatePrinterAddress,
                            label = { 
                                Text(
                                    when(uiState.connectionType) {
                                        PrinterConnectionType.TCP_IP -> "Dirección IP (Ej. 192.168.1.100)"
                                        PrinterConnectionType.BLUETOOTH -> "Dirección MAC Bluetooth"
                                        PrinterConnectionType.USB -> "Ruta USB (/dev/usb/lp0)"
                                        else -> "Dirección"
                                    }
                                ) 
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = viewModel::testPrinterConnection,
                            enabled = !uiState.isTestingConnection,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (uiState.isTestingConnection) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Text("Probar Conexión")
                            }
                        }
                    }
                }
            }
        }
    }
}
