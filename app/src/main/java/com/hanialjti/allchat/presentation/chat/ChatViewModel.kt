package com.hanialjti.allchat.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.hanialjti.allchat.data.model.Attachment
import com.hanialjti.allchat.data.model.MessageItem
import com.hanialjti.allchat.data.model.Participant
import com.hanialjti.allchat.data.remote.model.DownloadProgress
import com.hanialjti.allchat.data.remote.model.UploadProgress
import com.hanialjti.allchat.data.repository.ConversationRepository
import com.hanialjti.allchat.data.repository.FileRepository
import com.hanialjti.allchat.data.repository.IMessageRepository
import com.hanialjti.allchat.data.repository.UserRepository
import com.hanialjti.allchat.presentation.conversation.ContactImage
import com.hanialjti.allchat.presentation.conversation.UiText
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

class ChatViewModel(
    private val messageRepository: IMessageRepository,
    private val fileRepository: FileRepository,
    private val userRepository: UserRepository,
    private val conversationRepository: ConversationRepository,
    private val conversationId: String
) : ViewModel() {

    private val _chatUiState = MutableStateFlow(ChatScreenUiState())
    val uiState: StateFlow<ChatScreenUiState> get() = _chatUiState

    private var shouldCreateConversation = false
    private var lastReadMessage: MessageItem.MessageData? = null

    fun updateLastReadMessage(lastReadMessage: MessageItem.MessageData) {
        if (this.lastReadMessage == null || this.lastReadMessage != lastReadMessage && this.lastReadMessage?.isOlderThan(
                lastReadMessage
            ) == true
        ) {
            Timber.d("lastReadMessage updated. New value = $lastReadMessage")
            this.lastReadMessage = lastReadMessage
            viewModelScope.launch {
                messageRepository.sendSeenMarkerForMessage(lastReadMessage.id)
            }
        }
    }

    init {

        viewModelScope.launch {
            val chatDetails = conversationRepository
                .getChatDetails(conversationId)

            if (chatDetails != null) {
                _chatUiState.update {
                    it.copy(
                        name = chatDetails.name ?: defaultName,
                        image = chatDetails.avatar,
                        status = null, // TODO
                        isGroupChat = chatDetails.isGroupChat
                    )
                }
            } else {

                val userDetails = userRepository.getUserDetails(conversationId)

                _chatUiState.update {
                    it.copy(
                        name = userDetails?.name ?: defaultName,
                        image = userDetails?.avatar,
                        status = null, // TODO
                        isGroupChat = false
                    )
                }

                shouldCreateConversation = true
            }

        }

        viewModelScope.launch {
            messageRepository.resendAllPendingMessages()
        }

        viewModelScope.launch {
            fileRepository.getUploadProgressForAll()
                .collect { uploadProgresses ->
                    _chatUiState.update {
                        it.copy(
                            uploadProgresses = uploadProgresses
                        )
                    }
                }
        }

        viewModelScope.launch {
            userRepository.isBlocked(conversationId)
                .collectLatest { isBlocked ->
                    _chatUiState.update {
                        it.copy(isBlocked = isBlocked)
                    }
                }
        }

        viewModelScope.launch {
            fileRepository.getDownloadProgressForAll()
                .collect { downloadProgresses ->
                    _chatUiState.update {
                        it.copy(
                            downloadProgresses = downloadProgresses
                        )
                    }
                }
        }



    }

    fun setAllMessagesAsRead() {
        //TODO use GlobalScope
        viewModelScope.launch {
            messageRepository.setMessagesAsRead(conversationId)
        }
    }

    val messages = messageRepository
        .fetchMessagesFor(conversationId)
        .onEach {
            conversationRepository.resetUnreadCounter(conversationId)
        }
        .cachedIn(viewModelScope)


    fun downloadAttachment(messageData: MessageItem.MessageData) {
        viewModelScope.launch {
            messageRepository.downloadAttachment(messageData)
        }
    }

    private fun updateMyChatState(state: Participant.State) {
        viewModelScope.launch {
            conversationRepository.updateMyChatState(conversationId, state)
        }
    }

    private suspend fun addUserToContacts() {
        conversationRepository.addUserToContactList(conversationId)
    }

    fun sendMessage() = viewModelScope.launch {

        val textInput = _chatUiState.value.textInput
        val attachment = _chatUiState.value.attachment
        val replyingTo = _chatUiState.value.replyingTo
        val isGroupChat = _chatUiState.value.isGroupChat

        if (shouldCreateConversation) {
            addUserToContacts()
        }

        _chatUiState.update {
            it.copy(
                textInput = "",
                attachment = null,
                replyingTo = null
            )
        }

        if (textInput.isNotBlank() || attachment != null) {
            messageRepository.sendMessage(
                body = textInput,
                replyingTo = replyingTo?.id,
                contactId = conversationId,
                isGroupChat = isGroupChat,
                attachment = attachment
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
                Participant.State.Paused
            } else {
                Participant.State.Composing
            }
        )

    }

    fun blockUser() {
        viewModelScope.launch {
            userRepository.blockUser(conversationId)
        }
    }

    fun unblockUser() {
        viewModelScope.launch {
            userRepository.unblockUser(conversationId)
        }
    }

    fun createNewTempFile(fileExtension: String): File? =
        fileRepository.createNewTempFile(fileExtension = fileExtension)

    fun deleteCameraTempFile() =
        _chatUiState.value.lastCreatedTempFile?.let { fileRepository.deleteTempFile(it) }

    fun updateAttachment(attachment: Attachment?) {
        viewModelScope.launch {
            _chatUiState.update {
                it.copy(
                    attachment = attachment
                )
            }
        }
    }

    fun updateReplyingTo(replyingTo: MessageItem.MessageData?) {
        viewModelScope.launch {
            _chatUiState.update {
                it.copy(
                    replyingTo = replyingTo
                )
            }
        }
    }

    fun updateTempFile(tempFile: File?) {
        viewModelScope.launch {
            _chatUiState.update { it.copy(lastCreatedTempFile = tempFile) }
        }
    }

    fun updateIsChatMenuVisible(isChatMenuVisible: Boolean) {
        viewModelScope.launch {
            _chatUiState.update {
                it.copy(isChatMenuVisible = isChatMenuVisible)
            }
        }
    }


}

const val defaultName = "AllChat User"

data class ChatScreenUiState(
    val textInput: String = "",
    val attachment: Attachment? = null,
    val isBlocked: Boolean = false,
    val name: String? = null,
    val image: ContactImage? = null,
    val isGroupChat: Boolean = false,
    val status: UiText? = null,
    val replyingTo: MessageItem.MessageData? = null,
    val uploadProgresses: Map<Any, UploadProgress> = mapOf(),
    val downloadProgresses: Map<Any, DownloadProgress> = mapOf(),
    val lastCreatedTempFile: File? = null,
    val isChatMenuVisible: Boolean = false
)