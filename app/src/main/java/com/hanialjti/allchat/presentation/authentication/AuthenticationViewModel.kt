package com.hanialjti.allchat.presentation.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanialjti.allchat.R
import com.hanialjti.allchat.data.local.datastore.UserCredentials
import com.hanialjti.allchat.data.remote.InvalidUsernameOrPassword
import com.hanialjti.allchat.data.remote.model.CallResult
import com.hanialjti.allchat.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthenticationViewModel(
    private val authenticationRepository: AuthRepository,
//    private val userPreferencesManager: UserPreferencesManager,
//    private val connectionManager: ConnectionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthenticationUiState())
    val uiState: StateFlow<AuthenticationUiState> get() = _uiState

    init {

        viewModelScope.launch {
            authenticationRepository.connectedUser
                .collectLatest {
                    if (it != null) {
                        _uiState.update {
                            it.copy(
                                credentialsSaved = true
                            )
                        }
                    }
                }
        }
//        viewModelScope.launch {
//            authenticationUseCases.signIn(null)
//            connectionManager.observeConnectivityStatus()
//                .collectLatest { connectivityStatus ->
//                    Timber.d("Xmpp status: ${connectivityStatus.name}")
//                    if (connectivityStatus == ConnectionManager.Status.Connected) {
//                        val username = _uiState.value.username
//                        val password = _uiState.value.password
//                        if (username.isNotEmpty() && password.isNotEmpty())
//                            userPreferencesManager.updateUserCredentials(
//                                UserCredentials(
//                                    username,
//                                    password
//                                )
//                            )
//                    }
//                }
//        }

//        viewModelScope.launch {
//            userPreferencesManager.userCredentials
//                .collectLatest { userCredentials ->
//                    if (userCredentials?.username != null && userCredentials.password != null) {
//                        _uiState.update { it.copy(credentialsSaved = true) }
//                    }
//                }
//        }
    }

    private fun connect() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(loading = true)
            }
            val username = _uiState.value.username
            val password = _uiState.value.password
            val authResult = authenticationRepository.login(
                UserCredentials(
                    username,
                    password
                )
            )
            val message = if (authResult is CallResult.Error)
                when (authResult.cause) {
                    is InvalidUsernameOrPassword -> R.string.invalid_credentials
                    else -> null
                }
            else null
            _uiState.update {
                it.copy(
                    loading = false,
                    message = message
                )
            }
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

    fun updateShowPassword(isPasswordVisible: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPasswordVisible = isPasswordVisible) }
        }
    }

}

data class AuthenticationUiState(
    val username: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val message: Int? = null,
    val credentialsSaved: Boolean = false,
    val loading: Boolean = false
)