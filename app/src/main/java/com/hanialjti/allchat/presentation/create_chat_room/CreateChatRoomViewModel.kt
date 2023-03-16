package com.hanialjti.allchat.presentation.create_chat_room

import androidx.compose.foundation.pager.PagerState
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

class CreateChatRoomViewModel(
    private val conversationRepository: ConversationRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateChatRoomUiState())
    val uiState: StateFlow<CreateChatRoomUiState> get() = _uiState

    init {
        viewModelScope.launch {
            userRepository.getAllUsersByOwnerId()
                .collectLatest { userList ->
                    _uiState.update {
                        it.copy(
                            allUsers = userList.toSet()
                        )
                    }
                }
        }
    }

    fun createChatRoom() {
        viewModelScope.launch {
            val invitedUsers = _uiState.value.selectedUsers.map { it.id }.toSet()
            val name = _uiState.value.roomName
            conversationRepository.createChatRoom(name, "", invitedUsers)
        }
    }

    fun updateRoomName(name: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(roomName = name)
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

data class CreateChatRoomUiState(
    val allUsers: Set<User> = setOf(),
    val selectedUsers: Set<User> = setOf(),
    val currentStep: GroupChatCreationStep = GroupChatCreationStep.SelectInitialParticipants,
    val pagerState: PagerState = PagerState(),
    val message: String? = null,
    val roomName: String = "",
    val roomImage: String? = null,
    val isLoading: Boolean = false
)

enum class GroupChatCreationStep(val pageIndex: Int) {
    SelectInitialParticipants(0),
    InputGroupChatInfo(1)
}