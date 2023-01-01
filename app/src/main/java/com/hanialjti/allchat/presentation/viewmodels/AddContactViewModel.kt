package com.hanialjti.allchat.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanialjti.allchat.data.repository.ConversationRepository
import com.hanialjti.allchat.data.repository.UserRepository
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

            //TODO
            owner?.let {
//                val conversation = ConversationAndUser(
//                    conversationEntity = ContactEntity(
////                        id = id,
//                        isGroupChat = false,
//                        name = nickname,
//                        to = id,
//                        from = owner
//                    ),
//                    user = UserEntity(
//                        id = id,
//                        name = nickname
//                    )
//                )
//                conversationRepository.insertConversationAndUser(conversation)
            }

        }
    }
}

data class AddContactUiState(
    val owner: String? = null,
    val id: String = "",
    val nickname: String = "",
)