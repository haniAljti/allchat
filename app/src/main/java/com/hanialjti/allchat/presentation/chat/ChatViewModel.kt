package com.hanialjti.allchat.presentation.chat

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.hanialjti.allchat.data.model.Attachment
import com.hanialjti.allchat.data.model.ChatState
import com.hanialjti.allchat.data.model.MessageItem
import com.hanialjti.allchat.data.remote.model.DownloadProgress
import com.hanialjti.allchat.data.remote.model.UploadProgress
import com.hanialjti.allchat.data.repository.ConversationRepository
import com.hanialjti.allchat.data.repository.FileRepository
import com.hanialjti.allchat.data.repository.IMessageRepository
import com.hanialjti.allchat.data.repository.UserRepository
import com.hanialjti.allchat.domain.usecase.*
import com.hanialjti.allchat.presentation.conversation.ContactImage
import com.hanialjti.allchat.presentation.conversation.UiText
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

class ChatViewModel(
    private val chatRepository: IMessageRepository,
    private val fileRepository: FileRepository,
    private val userRepository: UserRepository,
    private val conversationRepository: ConversationRepository,
    private val resetUnreadCounterUseCase: ResetUnreadCounterUseCase,
    private val addUserToContactsUseCase: AddUserToContactsUseCase,
    private val getContactInfoUseCase: GetContactInfoUseCase,
    private val sendReadMarkerForMessageUseCase: SendReadMarkerForMessageUseCase,
    private val conversationId: String,
    private val isGroupChat: Boolean
) : ViewModel() {

    private val _chatUiState = MutableStateFlow(ChatScreenUiState())
    val uiState: StateFlow<ChatScreenUiState> get() = _chatUiState

    private var shouldCreateConversation = false
    private var lastReadMessage: MessageItem.MessageData? = null

    fun updateLastReadMessage(lastReadMessage: MessageItem.MessageData) {
        if (this.lastReadMessage == null || this.lastReadMessage != lastReadMessage && this.lastReadMessage?.isOlderThan(lastReadMessage) == true) {
            Timber.d("lastReadMessage updated. New value = $lastReadMessage")
            this.lastReadMessage = lastReadMessage
            viewModelScope.launch {
                sendReadMarkerForMessageUseCase(lastReadMessage)
            }
        }
    }

    init {
        updateMyChatState(ChatState.Active(conversationId))
        viewModelScope.launch {
            chatRepository.resendAllPendingMessages()
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

    val messages = chatRepository
        .fetchMessagesFor(conversationId)
        .onEach {
            resetUnreadCounterUseCase(conversationId)
        }
        .cachedIn(viewModelScope)


    @RequiresApi(Build.VERSION_CODES.Q)
    fun downloadAttachment(messageData: MessageItem.MessageData) {
        viewModelScope.launch {
            chatRepository.downloadAttachment(messageData)
        }
    }

    private fun updateMyChatState(chatState: ChatState) {
        viewModelScope.launch {
            conversationRepository.updateMyChatState(chatState)
        }
    }

    private suspend fun addUserToContacts() {
        addUserToContactsUseCase(conversationId)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun sendMessage() = viewModelScope.launch {

        val textInput = _chatUiState.value.textInput
        val attachment = _chatUiState.value.attachment
        val replyingTo = _chatUiState.value.replyingTo

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
            chatRepository.sendMessage(
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
                ChatState.Paused(conversationId)
            } else {
                ChatState.Composing(conversationId)
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

//    fun setThisChatAsInactive() {
//        updateMyChatState(ChatState.Inactive(conversationId))
//    }

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

}

const val defaultName = "AllChat User"

data class ChatScreenUiState(
    val textInput: String = "",
    val attachment: Attachment? = null,
    val isBlocked: Boolean = false,
    val name: String = defaultName,
    val image: ContactImage? = null,
    val status: UiText? = null,
    val replyingTo: MessageItem.MessageData? = null,
    val uploadProgresses: Map<Any, UploadProgress> = mapOf(),
    val downloadProgresses: Map<Any, DownloadProgress> = mapOf(),
    val lastCreatedTempFile: File? = null
)