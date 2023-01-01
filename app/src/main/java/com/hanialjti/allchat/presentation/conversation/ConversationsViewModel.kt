package com.hanialjti.allchat.presentation.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanialjti.allchat.data.repository.ConversationRepository
import com.hanialjti.allchat.domain.usecase.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ConversationsViewModel(
    private val syncChatsUseCase: SyncChatsUseCase,
    private val syncMessagesUseCase: SyncMessagesUseCase,
    private val getContactsUseCase: GetContactsUseCase,
    private val createChatRoomUseCase: CreateChatRoomUseCase,
    private val getConnectedUserUseCase: GetConnectedUserUseCase,
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    val contacts = getContactsUseCase()

    init {
        viewModelScope.launch {
            getConnectedUserUseCase().collectLatest {
                syncChatsUseCase()
                syncMessagesUseCase()
            }
        }
//        viewModelScope.launch { conversationRepository.listenForConversationUpdates() }
    }

    fun createChatRoom() {
        viewModelScope.launch {
            createChatRoomUseCase()
        }
    }

}