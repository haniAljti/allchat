package com.hanialjti.allchat.domain.usecase

import androidx.paging.PagingData
import androidx.paging.map
import com.hanialjti.allchat.R
import com.hanialjti.allchat.data.model.ContactWithLastMessage
import com.hanialjti.allchat.data.model.MessageSummary
import com.hanialjti.allchat.data.repository.ConversationRepository
import com.hanialjti.allchat.data.repository.UserRepository
import com.hanialjti.allchat.presentation.conversation.ContactContent
import com.hanialjti.allchat.presentation.conversation.UiText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest

class GetContactsUseCase(
    private val conversationRepository: ConversationRepository,
    private val userRepository: UserRepository
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<PagingData<ContactWithLastMessage>> = conversationRepository
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
        contactWithLastMessage: ContactWithLastMessage,
        lastMessage: MessageSummary?
    ): ContactContent? {

        return if (contactWithLastMessage.isGroupChat && contactWithLastMessage.composing.isNotEmpty()) {
            val users = userRepository.getUsers(contactWithLastMessage.composing)
            ContactContent.Composing(
                UiText.PluralStringResource(
                    R.plurals.composing,
                    contactWithLastMessage.composing.size,
                    users.joinToString()
                )
            )
        } else if (!contactWithLastMessage.isGroupChat && contactWithLastMessage.composing.isNotEmpty()) {
            ContactContent.Composing(UiText.StringResource(R.string.composing))
        } else {
            lastMessage.let { message ->
                message?.body?.let {
                    ContactContent.LastMessage(
                        text = UiText.DynamicString(message.body),
                        read = contactWithLastMessage.unreadMessages == 0
                    )
                }
            }
        }
    }
}