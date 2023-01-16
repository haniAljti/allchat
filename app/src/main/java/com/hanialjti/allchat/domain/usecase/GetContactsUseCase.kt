package com.hanialjti.allchat.domain.usecase

import androidx.paging.PagingData
import androidx.paging.map
import com.hanialjti.allchat.R
import com.hanialjti.allchat.data.model.Contact
import com.hanialjti.allchat.data.model.MessageSummary
import com.hanialjti.allchat.data.repository.ConversationRepository
import com.hanialjti.allchat.data.repository.UserRepository
import com.hanialjti.allchat.presentation.conversation.ContactContent
import com.hanialjti.allchat.presentation.conversation.UiText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest

class GetContactsUseCase(
    private val conversationRepository: ConversationRepository,
    private val userRepository: UserRepository
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<PagingData<Contact>> = conversationRepository
        .myContacts()
        .mapLatest { contactList ->
            contactList.map { contact ->
                contact.copy(
                    content = getConversationContent(
                        contact,
                        contact.lastMessage
                    )
                )
            }
        }


    private suspend fun getConversationContent(
        contact: Contact,
        lastMessage: MessageSummary?
    ): ContactContent? {

        return if (contact.isGroupChat && contact.composing.isNotEmpty()) {
            val users = userRepository.getUsers(contact.composing)
            ContactContent.Composing(
                UiText.PluralStringResource(
                    R.plurals.composing,
                    contact.composing.size,
                    users.joinToString()
                )
            )
        } else if (!contact.isGroupChat && contact.composing.isNotEmpty()) {
            ContactContent.Composing(UiText.StringResource(R.string.composing))
        } else {
            lastMessage.let { message ->
                message?.body?.let {
                    ContactContent.LastMessage(
                        text = UiText.DynamicString(message.body),
                        read = contact.unreadMessages == 0
                    )
                }
            }
        }
    }
}