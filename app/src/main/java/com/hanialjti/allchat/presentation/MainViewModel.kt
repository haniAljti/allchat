package com.hanialjti.allchat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanialjti.allchat.data.local.datastore.UserCredentials
import com.hanialjti.allchat.data.local.datastore.UserPreferencesManager
import com.hanialjti.allchat.data.remote.ConnectionManager
import com.hanialjti.allchat.data.repository.ConversationRepository
import com.hanialjti.allchat.data.repository.XmppChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val userPreferencesManager: UserPreferencesManager,
    private val connectionManager: ConnectionManager,
    private val chatRepository: XmppChatRepository,
    private val conversationRepository: ConversationRepository
) : ViewModel() {

//    private var isInBackground = true

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> get() = _uiState

    init {
        viewModelScope.launch { chatRepository.listenForMessageChanges() }
        viewModelScope.launch { chatRepository.observeChatStates() }
    }

//    override fun onResume(owner: LifecycleOwner) {
//        if (owner is Activity && !owner.isChangingConfigurations && isInBackground) {
//            isInBackground = false
//            viewModelScope.launch {
//                userPreferencesManager.userCredentials
//                    .collectLatest { userCredentials ->
//                        if (userCredentials != null) {
//                            connect(userCredentials)
//                            _uiState.update {
//                                it.copy(
//                                    isLoggedIn = true,
//                                    userCredentials = userCredentials
//                                )
//                            }
//                        }
//                    }
//            }
//        }
//    }

//    override fun onPause(owner: LifecycleOwner) {
//        when (owner) {
//            is Activity -> {
//                if (!owner.isChangingConfigurations) {
//                    isInBackground = true
//                    viewModelScope.launch { disconnect() }
//                }
//            }
//        }
//    }

//    suspend fun connect(userCredentials: UserCredentials?) {
//        if (userCredentials?.password != null && userCredentials.username != null) {
//            connectionManager.connect(userCredentials)
//        }
//    }

//    fun connect() {
//        viewModelScope.launch {
//            userPreferencesManager.userCredentials
//                .collectLatest { userCredentials ->
//                    if (userCredentials != null) {
//                        connectionManager.connect(userCredentials)
//                    }
//                }
//        }
//    }

    fun updateUserCredentials(userCredentials: UserCredentials) {
        viewModelScope.launch {
            userPreferencesManager.updateUserCredentials(userCredentials)
            _uiState.update {
                MainUiState(isLoggedIn = true, userCredentials = userCredentials)
            }
        }
    }

//    private fun disconnect() {
//        viewModelScope.launch { connectionManager.disconnect() }
//    }
}

data class MainUiState(
    val isLoggedIn: Boolean = false,
    val userCredentials: UserCredentials? = null
)