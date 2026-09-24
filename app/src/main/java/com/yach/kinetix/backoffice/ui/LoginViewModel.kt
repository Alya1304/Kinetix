package com.yach.kinetix.backoffice.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yach.kinetix.backoffice.data.dao.UserDao
import com.yach.kinetix.backoffice.data.entity.UserEntity
import com.yach.kinetix.backoffice.domain.AuthenticateUserUseCase
import com.yach.kinetix.core.security.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val users: List<UserEntity> = emptyList(),
    val selectedUser: UserEntity? = null,
    val pinInput: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoginSuccess: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userDao: UserDao,
    private val authenticateUserUseCase: AuthenticateUserUseCase,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        sessionManager.logout()
        loadUsers()
    }

    private fun loadUsers() {
        viewModelScope.launch {
            userDao.getAllActiveUsers().collect { userList ->
                _uiState.update { it.copy(users = userList) }
            }
        }
    }

    fun selectUser(user: UserEntity?) {
        _uiState.update { it.copy(selectedUser = user, pinInput = "", errorMessage = null) }
    }

    fun appendPin(digit: String) {
        _uiState.update { state ->
            if (state.pinInput.length < 4) {
                val newPin = state.pinInput + digit
                if (newPin.length == 4) {
                    // Auto-submit when 4 digits are entered
                    submitPin(newPin)
                    state.copy(pinInput = newPin)
                } else {
                    state.copy(pinInput = newPin, errorMessage = null)
                }
            } else {
                state
            }
        }
    }

    fun deletePinDigit() {
        _uiState.update { state ->
            if (state.pinInput.isNotEmpty()) {
                state.copy(pinInput = state.pinInput.dropLast(1), errorMessage = null)
            } else {
                state
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun submitPin(pin: String) {
        val user = _uiState.value.selectedUser ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            authenticateUserUseCase(user.username, pin)
                .onSuccess { authenticatedUser ->
                    sessionManager.loginUser(authenticatedUser)
                    _uiState.update { it.copy(isLoading = false, isLoginSuccess = true) }
                }
                .onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            errorMessage = error.message ?: "PIN Incorrecto",
                            pinInput = "" // clear PIN on error
                        ) 
                    }
                }
        }
    }
}
