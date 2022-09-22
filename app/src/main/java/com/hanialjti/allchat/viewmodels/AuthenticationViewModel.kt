package com.hanialjti.allchat.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanialjti.allchat.ConnectionManager
import com.hanialjti.allchat.datastore.UserPreferencesManager
import com.hanialjti.allchat.models.UserCredentials
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class AuthenticationViewModel(
    private val userPreferencesManager: UserPreferencesManager,
    private val connectionManager: ConnectionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthenticationUiState())
    val uiState: StateFlow<AuthenticationUiState> get() = _uiState

    init {
        viewModelScope.launch {
            connectionManager.observeConnectivityStatus()
                .collectLatest { connectivityStatus ->
                    Timber.d("Xmpp status: ${connectivityStatus.name}")
                    if (connectivityStatus == ConnectionManager.Status.Connected) {
                        val username = _uiState.value.username
                        val password = _uiState.value.password
                        if (username.isNotEmpty() && password.isNotEmpty())
                            userPreferencesManager.updateUserCredentials(
                                UserCredentials(
                                    username,
                                    password
                                )
                            )
                    }
                }
        }
        viewModelScope.launch {
            userPreferencesManager.userCredentials
                .collectLatest { userCredentials ->
                    if (userCredentials?.username != null && userCredentials.password != null) {
                        _uiState.update { it.copy(credentialsSaved = true) }
                    }
                }
        }
    }

    private fun connect() {
        viewModelScope.launch {
            val username = _uiState.value.username
            val password = _uiState.value.password
            connectionManager.connect(
                UserCredentials(
                    username,
                    password
                )
            )
        }
    }

    //TODO Add Validation
    fun login() {
        viewModelScope.launch {
            connect()
        }
    }

    fun updateUsername(username: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(username = username) }
        }
    }

    fun updatePassword(password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(password = password) }
        }
    }

}

data class AuthenticationUiState(
    val username: String = "",
    val password: String = "",
    val credentialsSaved: Boolean = false
)