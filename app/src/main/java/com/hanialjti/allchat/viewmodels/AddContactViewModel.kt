package com.hanialjti.allchat.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanialjti.allchat.models.ConversationAndUser
import com.hanialjti.allchat.models.entity.Conversation
import com.hanialjti.allchat.models.entity.User
import com.hanialjti.allchat.repository.ConversationRepository
import com.hanialjti.allchat.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AddContactViewModel constructor(
    private val conversationRepository: ConversationRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddContactUiState())
    val uiState: StateFlow<AddContactUiState> get() = _uiState

    fun updateId(id: String) = viewModelScope.launch { _uiState.update { it.copy(id = id) } }

    fun updateOwner(owner: String?) {
        viewModelScope.launch { _uiState.update { it.copy(owner = owner) } }
    }

    fun updateNickName(nickname: String) {
        viewModelScope.launch { _uiState.update { it.copy(nickname = nickname) } }
    }

    fun saveContact() {
        viewModelScope.launch {

            val id = _uiState.value.id
            val nickname = _uiState.value.nickname
            val owner = _uiState.value.owner

            owner?.let {
                val conversation = ConversationAndUser(
                    conversation = Conversation(
                        id = id,
                        isGroupChat = false,
                        name = nickname,
                        to = id,
                        from = owner
                    ),
                    user = User(
                        id = id,
                        name = nickname
                    )
                )
                conversationRepository.insertConversationAndUser(conversation)
            }

        }
    }
}

data class AddContactUiState(
    val owner: String? = null,
    val id: String = "",
    val nickname: String = "",
)