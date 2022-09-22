package com.hanialjti.allchat.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanialjti.allchat.ConnectionManager
import com.hanialjti.allchat.R
import com.hanialjti.allchat.datastore.UserPreferencesManager
import com.hanialjti.allchat.models.*
import com.hanialjti.allchat.repository.ConversationRepository
import com.hanialjti.allchat.repository.UserRepository
import com.hanialjti.allchat.repository.XmppChatRepository
import com.hanialjti.allchat.utils.getDefaultDrawableRes
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ConversationsViewModel constructor(
    private val conversationRepository: ConversationRepository,
    private val chatRepository: XmppChatRepository,
    private val userRepository: UserRepository,
    private val userPreferencesManager: UserPreferencesManager,
    private val connectionManager: ConnectionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationUiState())
    val uiState: StateFlow<ConversationUiState> get() = _uiState

    private val connectionState = connectionManager.observeConnectivityStatus()
        .combine(userPreferencesManager.username) { connectionStatus, username ->
            State(
                isConnected = connectionStatus == ConnectionManager.Status.Connected,
                owner = username
            )
        }

    init {
        viewModelScope.launch {
            connectionState
                .collectLatest {
                    if (it.isConnected && it.owner != null) {
                        conversationRepository.loadAllContacts(it.owner)
                    }
                }
        }
        viewModelScope.launch {
            userPreferencesManager.username
                .collectLatest {
                    it?.let { username ->
                        conversations(username)
                    }
                }
        }

        viewModelScope.launch {
            connectionState
                .collectLatest {
                    if (it.isConnected && it.owner != null) {
                        chatRepository.syncMessages(it.owner)
                    }
                }
        }
    }

    private fun conversations(owner: String) {
        viewModelScope.launch {
            conversationRepository
                .conversations(owner)
                .map { conversationAndUserList ->
                    conversationAndUserList
                        .sortedByDescending { it.conversation.lastUpdated }
                        .map { conversationAndUser ->
                            Contact(
                                id = conversationAndUser.conversation.id,
                                lastUpdated = conversationAndUser.conversation.lastUpdated,
                                name = conversationAndUser.name,
                                image = if (conversationAndUser.image != null) ContactImage.DynamicImage(
                                    conversationAndUser.image
                                ) else ContactImage.ImageRes(
                                    getDefaultDrawableRes(conversationAndUser.conversation.isGroupChat)
                                ),
                                unreadMessages = conversationAndUser.conversation.unreadMessages,
                                isOnline = !conversationAndUser.conversation.isGroupChat && conversationAndUser.user?.isOnline == true,
                                isGroupChat = conversationAndUser.conversation.isGroupChat,
                                content = getConversationContent(conversationAndUser)
                            )
                        }
                }
                .collectLatest { contactList ->
                    _uiState.update {
                        it.copy(contacts = contactList)
                    }
                }
        }
    }

    private suspend fun getConversationContent(conversationAndUser: ConversationAndUser): ContactInfo? {
        val composingUsers = conversationAndUser
            .conversation
            .otherComposingUsers

        return if (conversationAndUser.conversation.isGroupChat && composingUsers.isNotEmpty()) {
            val users = userRepository.getUsers(composingUsers)
            ContactInfo.Composing(
                UiText.PluralStringResource(
                    R.plurals.composing,
                    composingUsers.size,
                    users.joinToString()
                )
            )
        } else if (!conversationAndUser.conversation.isGroupChat && composingUsers.isNotEmpty()) {
            ContactInfo.Composing(UiText.StringResource(R.string.composing))
        } else {
            conversationAndUser.conversation.lastMessage?.let { message ->
                ContactInfo.LastMessage(
                    text = UiText.DynamicString(message),
                    read = conversationAndUser.conversation.unreadMessages == 0
                )
            }
        }
    }

    internal data class State(
        val isConnected: Boolean = false,
        val owner: String? = null
    )

}

data class ConversationUiState(
    val contacts: List<Contact> = listOf()
)