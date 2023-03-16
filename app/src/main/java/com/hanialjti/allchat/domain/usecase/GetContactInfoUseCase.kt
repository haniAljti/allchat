package com.hanialjti.allchat.domain.usecase

import com.hanialjti.allchat.R
import com.hanialjti.allchat.common.utils.asUiDate
import com.hanialjti.allchat.data.model.ContactWithLastMessage
import com.hanialjti.allchat.data.repository.ConversationRepository
import com.hanialjti.allchat.data.repository.UserRepository
import com.hanialjti.allchat.data.model.ContactInfo
import com.hanialjti.allchat.presentation.chat.defaultName
import com.hanialjti.allchat.presentation.conversation.UiText
import kotlinx.coroutines.flow.*

class GetContactInfoUseCase(
    private val conversationRepository: ConversationRepository,
    private val userRepository: UserRepository
) {

    suspend operator fun invoke(contactId: String): Flow<ContactInfo?> =
        conversationRepository.contact(contactId).map { contact ->
            contact ?: return@map null
            ContactInfo(
                name = contact.name ?: defaultName,
                image = contact.image,
                content = contact.getConversationContent()
            )
        }


    private suspend fun ContactWithLastMessage.getConversationContent(): UiText? {

        return if (isGroupChat && composing.isNotEmpty()) {
            val users = userRepository.getUsers(composing)
            UiText.PluralStringResource(
                R.plurals.composing,
                composing.size,
                users.joinToString()
            )
        } else if (!isGroupChat && composing.isNotEmpty()) {
            UiText.StringResource(R.string.composing)
        } else {
            to?.let { user ->
                val isOnline = user.isOnline
                val lastOnline = user.lastOnline
                if (isOnline) {
                    UiText.StringResource(R.string.online)
                } else
                    lastOnline
                        ?.asUiDate()
                        ?.asLastOnlineUiText()
            }
        }
    }
}