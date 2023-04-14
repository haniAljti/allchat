package com.hanialjti.allchat.presentation.invite_users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanialjti.allchat.data.model.User
import com.hanialjti.allchat.data.repository.ConversationRepository
import com.hanialjti.allchat.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class InviteUsersViewModel(
    private val chatRoomId: String,
    private val userRepository: UserRepository,
    private val conversationRepository: ConversationRepository
): ViewModel() {

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
                conversationRepository.inviteUserToChatRoom(it.id, chatRoomId)
            }
        }
    }

    fun addUserToInvitedList(user: User) {
        viewModelScope.launch {
            _uiState.update {
                val updatedList = it.selectedUsers.toMutableSet().apply { add(user) }
                it.copy(
                    selectedUsers = updatedList
                )
            }
        }
    }

    fun removeUserFromInvitedList(user: User) {
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
    val allUsers: Set<User> = setOf(),
    val selectedUsers: Set<User> = setOf(),
    val isUsersInvited: Boolean = false,
    val message: String? = null,
    val isLoading: Boolean = false
)