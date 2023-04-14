package com.hanialjti.allchat.presentation.chat_entity_details.chat_details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanialjti.allchat.data.remote.model.CallResult
import com.hanialjti.allchat.data.repository.ConversationRepository
import com.hanialjti.allchat.presentation.chat.defaultName
import com.hanialjti.allchat.presentation.conversation.ContactImage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class UpdateChatDetailsViewModel(
    private val conversationRepository: ConversationRepository,
    private val chatId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(UpdateChatInfoUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val contactInfo = conversationRepository.getChatDetails(chatId)

            contactInfo?.let { chatInfo ->
                _uiState.update { uiState ->
                    uiState.copy(
                        name = chatInfo.name ?: defaultName,
                        avatar = chatInfo.avatar,
                        description = chatInfo.description ?: ""
                    )
                }
            }
        }
    }

    fun updateName(name: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(name = name)
            }
        }
    }

    fun updateAvatar(avatarBytes: ByteArray) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(avatar = ContactImage.DynamicRawImage(avatarBytes))
            }
        }
    }

    fun updateDescription(description: String) {
        viewModelScope.launch {
            if (description.isNotBlank()) {
                _uiState.update {
                    it.copy(description = description)
                }
            }
        }
    }

    fun saveChanges() {
        viewModelScope.launch {
            val name = _uiState.value.name
            val description = _uiState.value.description
            val avatar = _uiState.value.avatar
            val updateResult = conversationRepository.updateRoomInfo(chatId, description, avatar, name)
            if (updateResult is CallResult.Success) {
                _uiState.update { it.copy(isUpdated = true) }
            } else _uiState.update { it.copy(errorMessage = "An error occurred while updating room info.") }
        }
    }

}

data class UpdateChatInfoUiState(
    val name: String = "",
    val avatar: ContactImage = ContactImage.DefaultProfileImage(false),
    val description: String = "",
    val isUpdated: Boolean = false,
    val errorMessage: String? = null
)