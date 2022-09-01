package com.hanialjti.allchat.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import androidx.paging.map
import com.hanialjti.allchat.R
import com.hanialjti.allchat.models.*
import com.hanialjti.allchat.models.entity.Media
import com.hanialjti.allchat.models.entity.Message
import com.hanialjti.allchat.repository.ChatRepository
import com.hanialjti.allchat.repository.ConversationRepository
import com.hanialjti.allchat.utils.asLocalDateTime
import com.hanialjti.allchat.utils.getDateText
import com.hanialjti.allchat.utils.getDefaultDrawableRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    private val _chatUiState = MutableStateFlow(ChatScreenUiState())
    val uiState: StateFlow<ChatScreenUiState> get() = _chatUiState

    fun getMessages(conversation: String) =
        chatRepository
            .messages(conversation)
            .map { messages ->
                messages.map {
                    UiMessage(
                        id = it.id,
                        body = it.body,
                        timestamp = it.timestamp,
                        from = it.from,
                        status = it.status,
                        readBy = it.readBy,
                        type = it.type,
                        attachment = it.media?.toAttachment() ?: it.location?.toAttachment(),
                    )
                }
            }.cachedIn(viewModelScope)

    fun saveMessageContentUri(message: UiMessage, cacheContentUri: String) {
        viewModelScope.launch {
            chatRepository.saveMessageContentUri(message.id, cacheContentUri)
        }
    }

    fun getAttachment(messageId: String) = chatRepository.getMessageById(messageId).map {
        it.media?.toAttachment()
    }

    fun sendMessage() {
        viewModelScope.launch {
            val textInput = _chatUiState.value.textInput
            val attachment = _chatUiState.value.attachment

            if (textInput != "" || attachment != null) {
                chatRepository.sendMessage(
                    Message(
                        body = textInput,
                        media = attachment?.toMedia(),
                        conversation = _chatUiState.value.conversation,
                        from = _chatUiState.value.owner,
                        status = "pending"
                    )
                )
            }

            _chatUiState.update {
                it.copy(
                    textInput = "",
                    attachment = null
                )
            }
        }
    }

    fun updateTextInput(text: String) {
        viewModelScope.launch {
            _chatUiState.update {
                it.copy(textInput = text)
            }
        }
    }

    fun setConversation(conversation: String, owner: String?) {
        viewModelScope.launch {
            _chatUiState.update {
                it.copy(
                    conversation = conversation,
                    owner = owner
                )
            }
            observeChatStatus(
                conversation
            )
        }
    }

    private fun observeChatStatus(conversationId: String) {
        viewModelScope.launch {
            conversationRepository
                .conversation(conversationId)
                .collectLatest { conversationAndUser ->
                    updateChatInfo(
                        name = conversationAndUser.name,
                        image = if (conversationAndUser.image != null) ContactImage.DynamicImage(
                            conversationAndUser.image
                        ) else ContactImage.ImageRes(getDefaultDrawableRes(conversationAndUser.conversation.isGroupChat)),
                        status = conversationAndUser.conversation.otherComposingUsers?.let { composing ->
                            UiText.PluralStringResource(
                                R.plurals.composing,
                                composing.count,
                                composing.userListString
                            )
                        } ?: conversationAndUser.user?.let {
                            if (it.isOnline) {
                                UiText.StringResource(
                                    R.string.online
                                )
                            } else if (it.lastOnline != null){
                                it.lastOnline.asLocalDateTime().getDateText().asLastOnlineUiText()
                            } else null
                        }
                    )
                }
        }
    }


    private fun updateChatInfo(
        name: String?,
        image: ContactImage?,
        status: UiText? = null
    ) {
        viewModelScope.launch {
            _chatUiState.update {
                it.copy(
                    name = name ?: defaultName,
                    image = image,
                    status = status
                )
            }
        }
    }

    private fun updateStatus(status: UiText? = null) {
        viewModelScope.launch {
            _chatUiState.update {
                it.copy(
                    status = status
                )
            }
        }
    }

    fun updateAttachment(attachment: UiAttachment?) {
        viewModelScope.launch {
            _chatUiState.update {
                it.copy(
                    attachment = attachment
                )
            }
        }
    }

    fun updateCurrentlyPlayingMedia(isPaused: Boolean = false) {
        viewModelScope.launch {
            _chatUiState.update {
                it.copy(
                    isPaused = isPaused
                )
            }
        }
    }

    fun updateTrackPosition(key: String, position: Int) {
        viewModelScope.launch {
            _chatUiState.update {
                it.copy(
                    trackPositions = it.trackPositions.apply {
                        put(key, position)
                    }
                )
            }
        }
    }
}

const val defaultName = "AllChat User"

data class Attachment(
    val name: String? = null,
    val type: Media.Type,
    val uri: Uri,
    val duration: Long? = null
)

data class ChatScreenUiState(
    val conversation: String? = null,
    val textInput: String = "",
    val attachment: UiAttachment? = null,
    val isPaused: Boolean = false,
    val owner: String? = null,
    val name: String = defaultName,
    val image: ContactImage? = null,
    val status: UiText? = null,
    val trackPositions: MutableMap<String, Int> = mutableMapOf()
)