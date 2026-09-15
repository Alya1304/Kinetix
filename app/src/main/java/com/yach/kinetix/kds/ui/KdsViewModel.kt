package com.yach.kinetix.kds.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import javax.inject.Inject

enum class TicketUrgency { NORMAL, WARNING, CRITICAL }

data class KdsItem(
    val id: UUID = UUID.randomUUID(),
    val quantity: Int,
    val name: String,
    val modifiers: List<String> = emptyList(),
    val isCompleted: Boolean = false
)

data class KdsTicket(
    val id: UUID = UUID.randomUUID(),
    val orderNumber: String,
    val tableName: String,
    val waiterName: String,
    val elapsedMinutes: Int,
    val urgency: TicketUrgency,
    val items: List<KdsItem>
)

data class KdsUiState(
    val stations: List<String> = listOf("Global", "Parrilla", "Bebidas", "Postres"),
    val selectedStationIndex: Int = 0,
    val activeTickets: List<KdsTicket> = emptyList()
)

@HiltViewModel
class KdsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(KdsUiState())
    val uiState: StateFlow<KdsUiState> = _uiState.asStateFlow()

    init {
        loadMockTickets()
    }

    private fun loadMockTickets() {
        val mockData = listOf(
            KdsTicket(
                orderNumber = "#1042",
                tableName = "Mesa 4",
                waiterName = "Carlos R.",
                elapsedMinutes = 4,
                urgency = TicketUrgency.NORMAL,
                items = listOf(
                    KdsItem(quantity = 1, name = "Hamburguesa Clásica", modifiers = listOf("Sin Cebolla", "Término Medio")),
                    KdsItem(quantity = 2, name = "Cerveza Artesanal", modifiers = emptyList())
                )
            ),
            KdsTicket(
                orderNumber = "#1041",
                tableName = "Mesa 12",
                waiterName = "Ana G.",
                elapsedMinutes = 14,
                urgency = TicketUrgency.WARNING,
                items = listOf(
                    KdsItem(quantity = 2, name = "Tacos de Ribeye", modifiers = listOf("Salsa Aparte")),
                    KdsItem(quantity = 1, name = "Guacamole Central", modifiers = emptyList(), isCompleted = true)
                )
            ),
            KdsTicket(
                orderNumber = "#1039",
                tableName = "Barra 2",
                waiterName = "Luis P.",
                elapsedMinutes = 26,
                urgency = TicketUrgency.CRITICAL,
                items = listOf(
                    KdsItem(quantity = 1, name = "Corte Tomahawk 1kg", modifiers = listOf("Término 3/4")),
                    KdsItem(quantity = 1, name = "Papas Trufadas", modifiers = emptyList())
                )
            ),
            KdsTicket(
                orderNumber = "#1044",
                tableName = "Para Llevar",
                waiterName = "Mostrador",
                elapsedMinutes = 2,
                urgency = TicketUrgency.NORMAL,
                items = listOf(
                    KdsItem(quantity = 3, name = "Pizza Margarita", modifiers = emptyList()),
                    KdsItem(quantity = 1, name = "Ensalada César", modifiers = listOf("Aderezo Extra"))
                )
            )
        )
        _uiState.update { it.copy(activeTickets = mockData) }
    }

    fun selectStation(index: Int) {
        _uiState.update { it.copy(selectedStationIndex = index) }
    }

    fun toggleItemCompletion(ticketId: UUID, itemId: UUID) {
        _uiState.update { state ->
            val updatedTickets = state.activeTickets.map { ticket ->
                if (ticket.id == ticketId) {
                    val updatedItems = ticket.items.map { item ->
                        if (item.id == itemId) item.copy(isCompleted = !item.isCompleted) else item
                    }
                    ticket.copy(items = updatedItems)
                } else {
                    ticket
                }
            }
            state.copy(activeTickets = updatedTickets)
        }
    }

    fun completeTicket(ticketId: UUID) {
        _uiState.update { state ->
            state.copy(activeTickets = state.activeTickets.filterNot { it.id == ticketId })
        }
    }
}
