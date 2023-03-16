package com.hanialjti.allchat.presentation.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.hanialjti.allchat.data.repository.ConversationRepository
import com.hanialjti.allchat.data.repository.IMessageRepository
import com.hanialjti.allchat.domain.usecase.*

class ConversationsViewModel(
    private val syncMessagesUseCase: SyncMessagesUseCase,
    private val messageRepository: IMessageRepository,
    private val getConnectedUserUseCase: GetConnectedUserUseCase,
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    val contacts = conversationRepository.myContacts().cachedIn(viewModelScope)

    private val syncedContacts = listOf<String>()

    fun syncMessages(chat: String) {
        if (!syncedContacts.contains(chat)) {
//            messageRepository.syncMessages()
        }
    }

}