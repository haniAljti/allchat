package com.hanialjti.allchat.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import com.hanialjti.allchat.R
import com.hanialjti.allchat.data.local.datastore.UserPreferencesManager
import com.hanialjti.allchat.models.*
import com.hanialjti.allchat.data.local.room.entity.*
import com.hanialjti.allchat.data.repository.ConversationRepository
import com.hanialjti.allchat.data.repository.UserRepository
import com.hanialjti.allchat.data.repository.XmppChatRepository
import com.hanialjti.allchat.common.utils.asLocalDateTime
import com.hanialjti.allchat.common.utils.asUiDate
import com.hanialjti.allchat.common.utils.getDefaultDrawableRes
import com.hanialjti.allchat.presentation.conversation.ChatState
import com.hanialjti.allchat.presentation.conversation.ContactImage
import com.hanialjti.allchat.presentation.conversation.UiText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(
    private val chatRepository: XmppChatRepository,
    private val conversationRepository: ConversationRepository,
    private val userRepository: UserRepository,
    private val userPreferencesManager: UserPreferencesManager,
    private val conversationId: String,
    private val isGroupChat: Boolean
) : ViewModel() {

    private val _chatUiState = MutableStateFlow(ChatScreenUiState())
    val uiState: StateFlow<ChatScreenUiState> get() = _chatUiState

    private val conversationAndUser = conversationRepository
        .conversation(conversationId)

    private val shouldCreateConversation = conversationAndUser
        .mapLatest {
            it == null && !isGroupChat
        }

    private val lastReceivedMessage = userPreferencesManager
        .username
        .filterNotNull()
        .flatMapLatest {
            chatRepository.observeLastMessageNotSentByOwner(it, conversationId)
        }

    init {
        updateMyChatState(ChatState.Active(conversationId))

        viewModelScope.launch {
            lastReceivedMessage
                .filterNotNull()
                .collectLatest { message ->
                    delay(100) // wait till all messages arrive
                    if (!message.body.isNullOrEmpty() && message.status != Status.Seen) {
                        chatRepository.markMessageAsDisplayed(message.id)
                    }
                }
        }

//        viewModelScope.launch {
//            shouldCreateConversation
//                .collectLatest {
//                    if (it)
//                }
//        }
    }

    val messages = userPreferencesManager
        .username
        .flatMapLatest {
            if (it != null) {
                chatRepository.resendAllPendingMessages(it)
            }
            chatRepository
                .messages(conversationId, it)
                .onEach {
                    resetUnreadCounter()
                }
                .distinctUntilChanged()
                .map { messages ->
                    messages
                        .filter { message ->
                            message.body != null || message.media != null || message.location != null
                        }
                        .map { message ->
                            UiMessage(
                                id = message.id,
                                body = message.body,
                                timestamp = message.timestamp,
                                from = message.from,
                                status = message.status,
                                readBy = message.readBy,
                                type = message.type,
                                attachment = message.media?.asAttachment()
                                    ?: message.location?.asAttachment(),
                            )
                        }
                }
        }
        .cachedIn(viewModelScope)

    private var _shouldInitializeConversation: Boolean = false

    fun saveMessageContentUri(message: UiMessage, cacheContentUri: String) {
        viewModelScope.launch {
            chatRepository.saveMessageContentUri(message.id, cacheContentUri)
        }
    }

    fun getAttachment(messageId: Int) =
        chatRepository.getMessageFlowById(messageId).map {
            it.media?.asAttachment()
        }

    private fun updateMyChatState(chatState: ChatState) {
        viewModelScope.launch {
            chatRepository.updateMyChatState(
                chatState
            )
        }
    }

    fun sendMessage() = viewModelScope.launch {

        val textInput = _chatUiState.value.textInput
        val attachment = _chatUiState.value.attachment
        val owner = _chatUiState.value.owner

        if (_shouldInitializeConversation) {
            createConversationAndUser()?.let { it1 ->
                conversationRepository.insertConversationAndUser(
                    it1
                )
            }
        }

        _chatUiState.update {
            it.copy(
                textInput = "",
                attachment = null
            )
        }

        if (textInput != "" || attachment != null) {
            val message = Message(
                body = textInput,
                media = attachment?.asMedia(),
                conversation = conversationId,
                from = owner,
                owner = owner,
                type = if (isGroupChat) Type.GroupChat else Type.Chat
            )
            chatRepository.sendMessage(message)
        }
    }

    private fun createConversationAndUser(): ConversationAndUser? {

        val contactImage = _chatUiState.value.image
        val owner = _chatUiState.value.owner

        val image: String? = if (contactImage is ContactImage.DynamicImage)
            contactImage.imageUrl else null

        val conversation = owner?.let {
            Conversation(
                id = conversationId,
                isGroupChat = isGroupChat,
                name = _chatUiState.value.name,
                imageUrl = image,
                from = it,
                to = if (isGroupChat) null else conversationId
            )
        }
        val user = User(
            id = conversationId,
            name = _chatUiState.value.name,
            image = image
        )
        return conversation?.let { ConversationAndUser(it, user) }
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

    fun initializeChat(
        conversationId: String,
        isGroupChat: Boolean,
        initialName: String? = null,
        initialImage: String? = null
    ) {
        viewModelScope.launch {
            _chatUiState.update {
                it.copy(
                    image = initialImage?.let { image -> ContactImage.DynamicImage(image) }
                        ?: ContactImage.ImageRes(drawableRes = if (isGroupChat) R.drawable.ic_group else R.drawable.ic_user),
                    name = initialName ?: defaultName,
                )
            }
            observeChatStatus(conversationId)
        }
        viewModelScope.launch {
            userPreferencesManager.username
                .collectLatest { username ->
                    _chatUiState.update { it.copy(owner = username) }
                }
        }

//        _conversationId = conversationId
//        _isGroupChat = isGroupChat
//        updateMyChatState(ChatState.Active(conversationId))
    }

    private fun resetUnreadCounter() {
        viewModelScope.launch { conversationRepository.resetUnreadCounter(conversationId) }
    }

    private fun observeChatStatus(conversationId: String) {
        viewModelScope.launch {
            conversationRepository
                .conversation(conversationId)
                .collectLatest { conversationAndUser ->

                    if (conversationAndUser == null) {
                        _shouldInitializeConversation = true
                        Timber.d("Conversation is not yet initialized!")
                    }

                    conversationAndUser?.let {
                        _chatUiState.update {
                            it.copy(
                                name = conversationAndUser.name ?: defaultName,
                                image = if (conversationAndUser.image != null) ContactImage.DynamicImage(
                                    conversationAndUser.image
                                ) else ContactImage.ImageRes(
                                    getDefaultDrawableRes(
                                        conversationAndUser.conversation.isGroupChat
                                    )
                                ),
                                status = getConversationContent(conversationAndUser)
                            )
                        }
                    }
                }
        }
    }

    private suspend fun getConversationContent(conversationAndUser: ConversationAndUser): UiText? {
        val composingUsers = conversationAndUser
            .conversation
            .otherComposingUsers

        return if (conversationAndUser.conversation.isGroupChat && composingUsers.isNotEmpty()) {
            val users = userRepository.getUsers(composingUsers)
            UiText.PluralStringResource(
                R.plurals.composing,
                composingUsers.size,
                users.joinToString()
            )
        } else if (!conversationAndUser.conversation.isGroupChat && composingUsers.isNotEmpty()) {
            UiText.StringResource(R.string.composing)
        } else {
            conversationAndUser.user?.let { user ->
                val isOnline = user.isOnline
                val lastOnline = user.lastOnline
                if (isOnline) {
                    UiText.StringResource(R.string.online)
                } else
                    lastOnline
                        ?.asLocalDateTime()
                        ?.asUiDate()
                        ?.asLastOnlineUiText()
            }
        }
    }

    fun setThisChatAsInactive() {
        updateMyChatState(ChatState.Inactive(conversationId))
    }

    fun updateAttachment(attachment: Attachment?) {
        viewModelScope.launch {
            _chatUiState.update {
                it.copy(
                    attachment = attachment
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

data class ChatScreenUiState(
    val textInput: String = "",
    val attachment: Attachment? = null,
    val name: String = defaultName,
    val owner: String? = null,
    val image: ContactImage? = null,
    val status: UiText? = null,
    val trackPositions: MutableMap<String, Int> = mutableMapOf()
)