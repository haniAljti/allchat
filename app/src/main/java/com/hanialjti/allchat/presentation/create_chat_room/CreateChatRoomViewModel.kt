package com.hanialjti.allchat.presentation.create_chat_room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanialjti.allchat.data.model.User
import com.hanialjti.allchat.domain.usecase.GetUsersUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CreateChatRoomViewModel(
    private val getUsersUseCase: GetUsersUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateChatRoomUiState())
    val uiState: StateFlow<CreateChatRoomUiState> get() = _uiState

    init {
        viewModelScope.launch {
            getUsersUseCase()
                .collectLatest { userList ->
                    _uiState.update {
                        it.copy(
                            allUsers = userList
                        )
                    }
                }
        }
    }

//    fun inviteSelectedUsers() {
//        viewModelScope.launch {
//            val invitedUsers = _uiState.value.selectedUsers.mapNotNull { it.id }
//            inviteUsersUseCase(chatRoomId, *invitedUsers.toTypedArray())
//        }
//    }

    fun addUserToInvitedList(user: User) {
        viewModelScope.launch {
            _uiState.update {
                val updatedList = it.selectedUsers.toMutableList().apply { add(user) }
                it.copy(
                    selectedUsers = updatedList
                )
            }
        }
    }

    fun removeUserFromInvitedList(user: User) {
        viewModelScope.launch {
            _uiState.update {
                val updatedList = it.selectedUsers.toMutableList().apply { remove(user) }
                it.copy(
                    selectedUsers = updatedList
                )
            }
        }
    }
}

data class CreateChatRoomUiState(
    val allUsers: List<User> = listOf(),
    val selectedUsers: List<User> = listOf(),
    val isUsersInvited: Boolean = false,
    val message: String? = null,
    val isLoading: Boolean = false
)