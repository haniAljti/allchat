package com.hanialjti.allchat.data.model

import com.hanialjti.allchat.common.utils.UiDate
import com.hanialjti.allchat.presentation.chat.Attachment
import com.hanialjti.allchat.presentation.conversation.UiText
import java.time.LocalDateTime
import java.util.UUID

sealed class MessageItem(val itemId: String = UUID.randomUUID().toString()) {
    data class MessageDateSeparator(val date: UiDate): MessageItem()
    data class NewMessagesSeparator(val date: UiText): MessageItem()
    data class MessageData(
        val id: String? = null,
        val body: String? = null,
        val timestamp: LocalDateTime = LocalDateTime.now(),
        val contactId: String? = null,
        val senderId: String?,
        val senderImage: String?,
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



data class MessageSummary(
    val body: String?,
    val status: MessageStatus,
    val timestamp: LocalDateTime,
    val attachmentType: Attachment.Type?,
    val isSentByMe: Boolean = false
)