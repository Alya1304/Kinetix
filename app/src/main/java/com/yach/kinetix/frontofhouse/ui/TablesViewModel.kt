package com.yach.kinetix.frontofhouse.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yach.kinetix.frontofhouse.data.dao.RoomDao
import com.yach.kinetix.frontofhouse.data.dao.TableDao
import com.yach.kinetix.frontofhouse.data.entity.RestaurantTableEntity
import com.yach.kinetix.frontofhouse.data.entity.RoomEntity
import com.yach.kinetix.frontofhouse.data.entity.TableStatus
import com.yach.kinetix.frontofhouse.domain.TransferTableOrderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class TablesUiState(
    val rooms: List<RoomEntity> = emptyList(),
    val tables: List<RestaurantTableEntity> = emptyList(),
    val selectedRoomId: UUID? = null,
    val selectedTableId: UUID? = null,
    val isLoading: Boolean = false,
    val feedbackMessage: String? = null,
    val isHeatmapModeActive: Boolean = false,
    val isEditModeActive: Boolean = false,
    val mockHeatScores: Map<UUID, Float> = emptyMap()
)

@HiltViewModel
class TablesViewModel @Inject constructor(
    private val tableDao: TableDao,
    private val roomDao: RoomDao,
    private val transferTableOrderUseCase: TransferTableOrderUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TablesUiState())
    val uiState: StateFlow<TablesUiState> = _uiState.asStateFlow()

    init {
        loadRoomsAndTables()
    }

    private fun loadRoomsAndTables() {
        viewModelScope.launch {
            roomDao.getAllRooms().collect { rooms ->
                _uiState.update { it.copy(rooms = rooms) }
            }
        }
        viewModelScope.launch {
            tableDao.getAllTables().collect { tables ->
                val scores = tables.associate { it.id to (0..100).random() / 100f }
                _uiState.update { it.copy(tables = tables, mockHeatScores = scores) }
            }
        }
    }

    fun selectRoom(roomId: UUID?) {
        _uiState.update { it.copy(selectedRoomId = roomId) }
        viewModelScope.launch {
            if (roomId == null) {
                tableDao.getAllTables().collect { tables ->
                    val scores = tables.associate { it.id to (0..100).random() / 100f }
                    _uiState.update { it.copy(tables = tables, mockHeatScores = scores) }
                }
            } else {
                roomDao.getTablesByRoom(roomId).collect { tables ->
                    val scores = tables.associate { it.id to (0..100).random() / 100f }
                    _uiState.update { it.copy(tables = tables, mockHeatScores = scores) }
                }
            }
        }
    }

    fun toggleHeatmapMode() {
        _uiState.update { it.copy(isHeatmapModeActive = !it.isHeatmapModeActive) }
    }

    fun selectTable(tableId: UUID?) {
        _uiState.update { it.copy(selectedTableId = tableId) }
    }

    fun transferOrder(sourceTableId: UUID, targetTableId: UUID, authorizedByUserId: UUID) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            transferTableOrderUseCase(sourceTableId, targetTableId, authorizedByUserId)
                .onSuccess {
                    _uiState.update {
                        it.copy(isLoading = false, feedbackMessage = "Mesa trasladada con éxito")
                    }
                }.onFailure { err ->
                    _uiState.update {
                        it.copy(isLoading = false, feedbackMessage = "Error al trasladar mesa: ${err.message}")
                    }
                }
        }
    }

    fun createRoom(name: String) {
        viewModelScope.launch {
            roomDao.insertRoom(RoomEntity(name = name))
        }
    }

    fun changeTableStatus(tableId: UUID, status: TableStatus) {
        viewModelScope.launch {
            tableDao.updateTableStatus(
                tableId = tableId,
                status = status,
                currentOrderId = null
            )
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    fun toggleEditMode() {
        _uiState.update { it.copy(isEditModeActive = !it.isEditModeActive) }
    }

    fun updateTablePosition(tableId: UUID, newX: Float, newY: Float) {
        viewModelScope.launch {
            val table = tableDao.getTableById(tableId) ?: return@launch
            tableDao.updateTable(table.copy(positionX = newX, positionY = newY))
        }
    }
    
    fun addTable(tableNumber: String, capacity: Int) {
        val roomId = _uiState.value.selectedRoomId ?: _uiState.value.rooms.firstOrNull()?.id ?: return
        viewModelScope.launch {
            tableDao.insertTable(
                RestaurantTableEntity(
                    tableNumber = tableNumber,
                    roomId = roomId,
                    capacity = capacity,
                    positionX = 100f,
                    positionY = 100f
                )
            )
        }
    }
}
