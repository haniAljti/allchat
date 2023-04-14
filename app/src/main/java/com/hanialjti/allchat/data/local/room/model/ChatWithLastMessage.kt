package com.hanialjti.allchat.data.local.room.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.hanialjti.allchat.data.local.room.entity.ChatEntity
import com.hanialjti.allchat.data.local.room.entity.UserEntity
import com.hanialjti.allchat.data.model.*
import com.hanialjti.allchat.presentation.conversation.ContactImage
import kotlinx.datetime.*
import java.time.OffsetDateTime
import java.time.ZoneId

data class ChatWithLastMessage(
    @Embedded("chat_")
    val chat: ChatEntity,
    @Embedded("user_")
    val user: UserEntity?,
    @Embedded("message_")
    val lastMessage: MessageSummaryEntity?,
    val avatar: String?,
    val nickname: String?
)

data class ParticipantInfo(
    val chatId: String,
    val userId: String,
    val state: Participant.State = Participant.State.Inactive,
    val role: Role = Role.Participant,
    val cachePath: String?,
    val nickname: String?
)

data class MessageSummaryEntity(
    val body: String?,
    val status: MessageStatus,
    val timestamp: OffsetDateTime?,
    val attachment: Attachment?,
    @ColumnInfo(name = "sent_by_me")
    val isSentByMe: Boolean = false
)

fun MessageSummaryEntity.toMessageSummary() = MessageSummary(
    body = body,
    status = status,
    timestamp = timestamp?.atZoneSameInstant(ZoneId.systemDefault())?.toLocalDateTime()?.toKotlinLocalDateTime() ?: Clock.System.now().toLocalDateTime(
        TimeZone.currentSystemDefault()),
    attachmentType = attachment?.type,
    isSentByMe = isSentByMe
)

fun ChatWithLastMessage.toContact() = ContactWithLastMessage(
    id = chat.id,
    isGroupChat = chat.isGroupChat,
    name = nickname,
    image = avatar?.let { ContactImage.DynamicImage(it) }
        ?: ContactImage.DefaultProfileImage(chat.isGroupChat),
    isOnline = user?.isOnline ?: false,
    lastMessage = lastMessage?.toMessageSummary(),
    unreadMessages = chat.unreadMessages,
)