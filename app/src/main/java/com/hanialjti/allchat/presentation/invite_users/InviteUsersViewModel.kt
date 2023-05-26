package com.hanialjti.allchat.presentation.invite_users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanialjti.allchat.data.model.User
import com.hanialjti.allchat.data.model.UserDetails
import com.hanialjti.allchat.data.repository.ConversationRepository
import com.hanialjti.allchat.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonClassDiscriminator

class InviteUsersViewModel(
    private val chatRoomId: String,
    private val userRepository: UserRepository,
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InviteUsersUiState())
    val uiState: StateFlow<InviteUsersUiState> get() = _uiState

    init {
        viewModelScope.launch {
            userRepository.getAllUsers()
                .collectLatest { userList ->
                    _uiState.update {
                        it.copy(
                            allUsers = userList.toSet()
                        )
                    }
                }
        }
    }

    fun inviteSelectedUsers() {
        viewModelScope.launch {
            _uiState.value.selectedUsers.forEach {
                it.id?.let {
                    conversationRepository.inviteUserToChatRoom(it, chatRoomId)
                }
            }
        }
    }

    fun addUserToInvitedList(user: UserDetails) {
        viewModelScope.launch {
            _uiState.update {
                val updatedList = it.selectedUsers.toMutableSet().apply { add(user) }
                it.copy(
                    selectedUsers = updatedList
                )
            }
        }
    }

    fun removeUserFromInvitedList(user: UserDetails) {
        viewModelScope.launch {
            _uiState.update {
                val updatedList = it.selectedUsers.toMutableSet().apply { remove(user) }
                it.copy(
                    selectedUsers = updatedList
                )
            }
        }
    }
}

data class InviteUsersUiState(
    val allUsers: Set<UserDetails> = setOf(),
    val selectedUsers: Set<UserDetails> = setOf(),
    val isUsersInvited: Boolean = false,
    val message: String? = null,
    val isLoading: Boolean = false
)