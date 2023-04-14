package com.hanialjti.allchat.presentation.chat_entity_details.chat_details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanialjti.allchat.data.model.ParticipantInfo
import com.hanialjti.allchat.data.repository.ConversationRepository
import com.hanialjti.allchat.presentation.chat.defaultName
import com.hanialjti.allchat.presentation.conversation.ContactImage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class ChatDetailsViewModel(
    private val conversationRepository: ConversationRepository,
    private val chatId: String
) : ViewModel() {

    private val _infoUiState = MutableStateFlow(InfoUiState())
    val infoUiState = _infoUiState.asStateFlow()

    init {
        viewModelScope.launch {
            val contactInfo = conversationRepository.getChatDetailsStream(chatId)

            contactInfo.collectLatest { chatInfo ->
                _infoUiState.update { uiState ->
                    uiState.copy(
                        name = chatInfo?.name ?: defaultName,
                        avatar = chatInfo?.avatar ?: ContactImage.DefaultProfileImage(true),
                        description = chatInfo?.description,
                        participants = chatInfo?.participants ?: setOf(),
                        createdBy = chatInfo?.createdBy,
                        createdAt = chatInfo?.createdAt
                    )
                }
            }
        }
    }

}

data class InfoUiState(
    val name: String = "",
    val avatar: ContactImage = ContactImage.DefaultProfileImage(true),
    val description: String? = null,
    val participants: Set<ParticipantInfo> = setOf(),
    val createdBy: ParticipantInfo? = null,
    val createdAt: LocalDateTime? = null
)