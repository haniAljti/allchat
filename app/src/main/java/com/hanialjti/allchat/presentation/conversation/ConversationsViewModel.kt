package com.hanialjti.allchat.presentation.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.hanialjti.allchat.data.remote.AllChatSynchronizer
import com.hanialjti.allchat.data.repository.AuthRepository
import com.hanialjti.allchat.data.repository.ConversationRepository
import com.hanialjti.allchat.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ConversationsViewModel(
    private val synchronizer: AllChatSynchronizer,
    private val conversationRepository: ConversationRepository,
    private val authenticationRepository: AuthRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _conversationsUiState = MutableStateFlow(ConversationsUiState())
    val conversationsUiState = _conversationsUiState.asStateFlow()

    val contacts = conversationRepository.myContacts().cachedIn(viewModelScope)

    fun synchronize() {
        viewModelScope.launch {
            authenticationRepository
                .connectedUser
                .collectLatest {
                    if (it != null) {
                        synchronizer.synchronize()
                    }
                }
        }
        viewModelScope.launch {
            preferencesRepository
                .isSynced
                .collectLatest { isSynced ->
                    _conversationsUiState.update {
                        it.copy(isSynchronizing = !isSynced)
                    }
                }
        }
    }

    fun updateIsCreateChatMenuOpen(isOpen: Boolean) {
        viewModelScope.launch {
            _conversationsUiState.update {
                it.copy(isCreateChatMenuOpen = isOpen)
            }
        }
    }

}

data class ConversationsUiState(
    val isSynchronizing: Boolean = false,
    val isCreateChatMenuOpen: Boolean = false
)