package com.hanialjti.allchat.domain.usecase

import com.hanialjti.allchat.data.local.room.entity.MessageEntity
import com.hanialjti.allchat.data.model.MessageType
import com.hanialjti.allchat.data.repository.IChatRepository
import com.hanialjti.allchat.data.repository.UserRepository
import com.hanialjti.allchat.presentation.chat.Attachment
import kotlinx.coroutines.flow.first
import timber.log.Timber

class SendMessageUseCase(
    private val chatRepository: IChatRepository,
    private val userRepository: UserRepository
) {

    suspend operator fun invoke(
        body: String?,
        attachment: Attachment?,
        contactId: String,
        isGroupChat: Boolean
    ) {
        val user = userRepository.loggedInUser.first()

        if (user == null) {
            Timber.e("User is not logged in")
            return
        }

        chatRepository.sendMessage(
            MessageEntity(
                body = body,
                attachment = attachment?.asAttachmentEntity(),
                contactId = contactId,
                type = if (isGroupChat) MessageType.GroupChat else MessageType.Chat,
                ownerId = user.id,
                senderId = user.id
            )
        )
    }
}