package com.hanialjti.allchat.data.local.room.model

import com.hanialjti.allchat.common.utils.getDefaultDrawableRes
import com.hanialjti.allchat.data.local.room.entity.AttachmentEntity
import com.hanialjti.allchat.data.local.room.entity.MessageEntity
import com.hanialjti.allchat.data.local.room.entity.asMessageSummary
import com.hanialjti.allchat.data.model.Contact
import com.hanialjti.allchat.data.model.Marker
import com.hanialjti.allchat.data.model.MessageStatus
import com.hanialjti.allchat.presentation.chat.Attachment
import com.hanialjti.allchat.presentation.conversation.ContactImage
import java.time.OffsetDateTime

data class ChatEntry(
    val id: String,
    val isGroupChat: Boolean,
    val name: String?,
    val image: String?,
    val lastMessage: String?,
    val lastMessageTimestamp: OffsetDateTime?,
    val lastMessageStatus: MessageStatus?,
    val lastMessageAttachmentType: Attachment.Type?,
    val isLastMessageSentByMe: Boolean,
    val unreadMessages: Int,
)

fun ChatEntry.asContact() = Contact(
    id = id,
    isGroupChat = isGroupChat,
    name = name,
    image = getContactImage(),
    lastMessage = lastMessageStatus?.let { status ->
        lastMessageTimestamp?.let { timestamp ->
            MessageEntity(
                body = lastMessage,
                status = status,
                timestamp = timestamp,
                attachment = AttachmentEntity(type = lastMessageAttachmentType)
            ).asMessageSummary().copy(isSentByMe = isLastMessageSentByMe)
        }
    },
    unreadMessages = unreadMessages,
)

fun ChatEntry.getContactImage(): ContactImage {
    val image = image
    return if (image != null) ContactImage.DynamicImage(image) else
        ContactImage.ImageRes(getDefaultDrawableRes(isGroupChat))
}