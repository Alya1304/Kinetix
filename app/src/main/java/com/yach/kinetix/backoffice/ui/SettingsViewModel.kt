package com.yach.kinetix.backoffice.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yach.kinetix.core.hardware.HardwareSettingsManager
import com.yach.kinetix.core.hardware.PrinterConnectionType
import com.yach.kinetix.core.hardware.PrinterService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val connectionType: PrinterConnectionType = PrinterConnectionType.NONE,
    val printerAddress: String = "",
    val restaurantName: String = "",
    val ticketHeader: String = "",
    val isTestingConnection: Boolean = false,
    val testResultMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: HardwareSettingsManager,
    private val printerService: PrinterService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        _uiState.update {
            it.copy(
                connectionType = settingsManager.printerConnectionType,
                printerAddress = settingsManager.printerAddress,
                restaurantName = settingsManager.restaurantName,
                ticketHeader = settingsManager.ticketHeader
            )
        }
    }

    fun updateConnectionType(type: PrinterConnectionType) {
        settingsManager.printerConnectionType = type
        _uiState.update { it.copy(connectionType = type) }
    }

    fun updatePrinterAddress(address: String) {
        settingsManager.printerAddress = address
        _uiState.update { it.copy(printerAddress = address) }
    }

    fun updateRestaurantName(name: String) {
        settingsManager.restaurantName = name
        _uiState.update { it.copy(restaurantName = name) }
    }

    fun updateTicketHeader(header: String) {
        settingsManager.ticketHeader = header
        _uiState.update { it.copy(ticketHeader = header) }
    }

    fun testPrinterConnection() {
        val address = _uiState.value.printerAddress
        if (address.isBlank()) {
            _uiState.update { it.copy(testResultMessage = "La dirección de la impresora no puede estar vacía") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isTestingConnection = true, testResultMessage = null) }
            // Simulamos la prueba de impresión como solicitó el usuario, o intentamos conectar
            printerService.connect(address).onSuccess {
                printerService.disconnect()
                _uiState.update { 
                    it.copy(
                        isTestingConnection = false, 
                        testResultMessage = "¡Conexión exitosa a $address!"
                    ) 
                }
            }.onFailure { err ->
                _uiState.update { 
                    it.copy(
                        isTestingConnection = false, 
                        testResultMessage = "Fallo de conexión: ${err.message}"
                    ) 
                }
            }
        }
    }
    
    fun clearMessage() {
        _uiState.update { it.copy(testResultMessage = null) }
    }
}
