package com.hanialjti.allchat.viewmodels

import android.app.Activity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanialjti.allchat.ConnectionManager
import com.hanialjti.allchat.datastore.UserPreferencesManager
import com.hanialjti.allchat.models.UserCredentials
import com.hanialjti.allchat.repository.XmppChatRepository
import com.hanialjti.allchat.repository.ConversationRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

class MainViewModel(
    private val userPreferencesManager: UserPreferencesManager,
    private val connectionManager: ConnectionManager,
    private val chatRepository: XmppChatRepository,
    private val conversationRepository: ConversationRepository
) : ViewModel(), DefaultLifecycleObserver {

    private var isInBackground = true

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> get() = _uiState

    init {
        viewModelScope.launch { chatRepository.listenForMessageChanges() }
        viewModelScope.launch { chatRepository.observeChatStates() }
    }

    override fun onResume(owner: LifecycleOwner) {
        if (owner is Activity && !owner.isChangingConfigurations && isInBackground) {
            isInBackground = false
            viewModelScope.launch {
                userPreferencesManager.userCredentials
                    .collectLatest { userCredentials ->
                        if (userCredentials != null) {
                            connect(userCredentials)
                            _uiState.update {
                                it.copy(
                                    isLoggedIn = true,
                                    userCredentials = userCredentials
                                )
                            }
                        }
                    }
            }
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        when (owner) {
            is Activity -> {
                if (!owner.isChangingConfigurations) {
                    isInBackground = true
                    viewModelScope.launch { disconnect() }
                }
            }
        }
    }

    suspend fun connect(userCredentials: UserCredentials?) {
        if (userCredentials?.password != null && userCredentials.username != null) {
            connectionManager.connect(userCredentials)
        }
    }

    fun connect() {
        viewModelScope.launch {
            userPreferencesManager.userCredentials
                .collectLatest { userCredentials ->
                    if (userCredentials != null) {
                        connectionManager.connect(userCredentials)
                    }
                }
        }
    }

    fun updateUserCredentials(userCredentials: UserCredentials) {
        viewModelScope.launch {
            userPreferencesManager.updateUserCredentials(userCredentials)
            _uiState.update {
                MainUiState(isLoggedIn = true, userCredentials = userCredentials)
            }
        }
    }

    private fun disconnect() {
        viewModelScope.launch { connectionManager.disconnect() }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("Main View Model cleared")
    }
}

data class MainUiState(
    val isLoggedIn: Boolean = false,
    val userCredentials: UserCredentials? = null
)