package com.hanialjti.allchat.data.model

import com.hanialjti.allchat.common.utils.UiDate
import com.hanialjti.allchat.presentation.chat.Attachment
import com.hanialjti.allchat.presentation.conversation.ContactImage
import com.hanialjti.allchat.presentation.conversation.UiText
import kotlinx.datetime.*
import java.util.UUID

sealed class MessageItem(val itemId: String = UUID.randomUUID().toString()) {
    data class MessageDateSeparator(val date: UiDate): MessageItem()
    data class NewMessagesSeparator(val date: UiText): MessageItem()
    data class MessageData(
        val id: String? = null,
        val body: String? = null,
        val timestamp: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
        val contactId: String? = null,
        val senderId: String?,
        val senderImage: ContactImage?,
        val senderName: String?,
        val ownerId: String? = null,
        val status: MessageStatus = MessageStatus.Pending,
        val type: MessageType? = null,
        val read: Boolean = false,
        val attachment: Attachment?,
    ): MessageItem() {
        fun isFromMe() = (senderId ?: ownerId) == ownerId
    }
}

enum class MessageStatus(val value: Int) {
    Pending(0),
    Error(1),
    Sending(2),
    Sent(3),
    Delivered(4),
    Seen(5);

    companion object {
        fun max(first: MessageStatus, second: MessageStatus) = if (first.value >= second.value) first else second
    }
}

enum class MessageType {
    Chat,
    GroupChat
}