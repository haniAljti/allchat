package com.hanialjti.allchat.presentation.chat

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.hanialjti.allchat.data.model.ChatState
import com.hanialjti.allchat.data.model.MessageItem
import com.hanialjti.allchat.data.repository.IChatRepository
import com.hanialjti.allchat.domain.usecase.*
import com.hanialjti.allchat.presentation.conversation.ContactImage
import com.hanialjti.allchat.presentation.conversation.UiText
import com.hanialjti.allchat.presentation.preview_attachment.PreviewAttachmentViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

class ChatViewModel(
    private val chatRepository: IChatRepository,
    private val resetUnreadCounterUseCase: ResetUnreadCounterUseCase,
    getMessagesUseCase: GetMessagesUseCase,
    private val addUserToContactsUseCase: AddUserToContactsUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val getContactInfoUseCase: GetContactInfoUseCase,
    private val sendReadMarkerForMessageUseCase: SendReadMarkerForMessageUseCase,
    private val conversationId: String,
    private val isGroupChat: Boolean
) : ViewModel(), PreviewAttachmentViewModel {

    private val _chatUiState = MutableStateFlow(ChatScreenUiState())
    val uiState: StateFlow<ChatScreenUiState> get() = _chatUiState

    private var shouldCreateConversation = false
    private var lastReadMessage: MessageItem.MessageData? = null

    fun updateLastReadMessage(lastReadMessage: MessageItem.MessageData) {
        if (this.lastReadMessage == null || this.lastReadMessage != lastReadMessage && this.lastReadMessage?.timestamp?.isBefore(
                lastReadMessage.timestamp
            ) == true
        ) {
            Timber.d("lastReadMessage updated. New value = $lastReadMessage")
            this.lastReadMessage = lastReadMessage
            viewModelScope.launch {
                sendReadMarkerForMessageUseCase(lastReadMessage)
            }
        }
    }

    init {
//        updateMyChatState(ChatState.Active(conversationId))
//        viewModelScope.launch {
//            connectionManager.getUsername()?.let { chatRepository.resendAllPendingMessages(it) }
//        }

        viewModelScope.launch {
            getContactInfoUseCase(conversationId)
                .collectLatest { contactInfo ->
                    if (contactInfo != null) {
                        _chatUiState.update {
                            it.copy(
                                name = contactInfo.name,
                                image = contactInfo.image,
                                status = contactInfo.content
                            )
                        }
                    } else {
                        shouldCreateConversation = !isGroupChat
                    }
                }
        }

    }

    fun setAllMessagesAsRead() {
        //TODO use GlobalScope
        viewModelScope.launch {
            chatRepository.setMessagesAsRead(conversationId)
        }
    }

    val messages = getMessagesUseCase(conversationId)
        .onEach {
            resetUnreadCounterUseCase(conversationId)
        }
        .cachedIn(viewModelScope)


    fun saveMessageContentUri(message: MessageItem.MessageData, cacheContentUri: String) {
        viewModelScope.launch {
//            chatRepository.saveMessageContentUri(message.id, cacheContentUri)
        }
    }

//    fun getAttachment(messageId: String) = {
//        getAttachmentUseCase(messageId).map {
//            it.attachment?.asAttachment()
//        }
//    }

    private fun updateMyChatState(chatState: ChatState) {
        viewModelScope.launch {
            chatRepository.updateMyChatState(chatState)
        }
    }

    private suspend fun addUserToContacts() {
        val userName = _chatUiState.value.name
        val userImage = _chatUiState.value.image
        val imageUrl = if (userImage is ContactImage.DynamicImage) userImage.imageUrl else null
        addUserToContactsUseCase(conversationId, userName, imageUrl)
    }

    fun sendMessage() = viewModelScope.launch {

        val textInput = _chatUiState.value.textInput
        val attachment = _chatUiState.value.attachment

        if (shouldCreateConversation) {
            addUserToContacts()
        }

        _chatUiState.update {
            it.copy(
                textInput = "",
                attachment = null
            )
        }

        if (textInput != "" || attachment != null) {
            sendMessageUseCase(
                body = textInput,
                attachment = attachment,
                contactId = conversationId,
                isGroupChat = isGroupChat
            )
        }
    }

    fun updateTextInput(text: String) {
        viewModelScope.launch {
            _chatUiState.update {
                it.copy(textInput = text)
            }
        }

        updateMyChatState(
            if (text.isEmpty()) {
                ChatState.Paused(conversationId)
            } else {
                ChatState.Composing(conversationId)
            }
        )

    }

//    fun setThisChatAsInactive() {
//        updateMyChatState(ChatState.Inactive(conversationId))
//    }

    fun updateAttachment(attachment: Attachment?) {
        viewModelScope.launch {
            _chatUiState.update {
                it.copy(
                    attachment = attachment
                )
            }
        }
    }

//    fun getRoomInfo() {
//        viewModelScope.launch {
//            conversationRepository.getRoomInfo(conversationId)
//        }
//    }

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

    override fun getAttachment(): State<Attachment?> {
        return mutableStateOf(_chatUiState.value.attachment)
    }

    override fun deleteAttachment() {
        updateAttachment(null)
    }
}

const val defaultName = "AllChat User"

data class ChatScreenUiState(
    val textInput: String = "",
    val attachment: Attachment? = null,
    val name: String = defaultName,
    val image: ContactImage? = null,
    val status: UiText? = null,
    val trackPositions: MutableMap<String, Int> = mutableMapOf()
)