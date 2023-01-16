package com.hanialjti.allchat.data.local.room.model

import com.hanialjti.allchat.data.local.room.entity.AvatarEntity
import com.hanialjti.allchat.data.model.MessageStatus
import com.hanialjti.allchat.presentation.chat.Attachment
import java.time.OffsetDateTime

data class ChatEntry(
    val id: String,
    val isGroupChat: Boolean,
    val name: String?,
    val avatarData: String?,
    val avatarSource: AvatarEntity?,
    val avatarMimeType: String?,
    val lastMessage: String?,
    val lastMessageTimestamp: OffsetDateTime?,
    val lastMessageStatus: MessageStatus?,
    val lastMessageAttachmentType: Attachment.Type?,
    val isLastMessageSentByMe: Boolean,
    val unreadMessages: Int,
)

//fun ChatEntry.asContact() = Contact(
//    id = id,
//    isGroupChat = isGroupChat,
//    name = name,
//    image = getContactImage(),
//    lastMessage = MessageSummary(
//        body = lastMessage,
//        status = lastMessageStatus,
//
//    ) lastMessageStatus?.let { status ->
//        lastMessageTimestamp?.let { timestamp ->
//            MessageEntity(
//                body = lastMessage,
//                status = status,
//                timestamp = timestamp,
//                attachment = AttachmentEntity(type = lastMessageAttachmentType)
//            ).asMessageSummary().copy(isSentByMe = isLastMessageSentByMe)
//        }
//    },
//    unreadMessages = unreadMessages,
//)