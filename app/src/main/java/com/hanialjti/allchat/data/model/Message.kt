package com.hanialjti.allchat.data.model

import com.hanialjti.allchat.R
import com.hanialjti.allchat.common.utils.UiDate
import com.hanialjti.allchat.presentation.conversation.ContactImage
import com.hanialjti.allchat.presentation.conversation.UiText
import kotlinx.datetime.*

sealed class MessageItem(open val itemId: String?) {
    data class MessageDateSeparator(val date: UiDate, override val itemId: String) :
        MessageItem(itemId)

    data class NewMessagesSeparator(
        val date: UiText = UiText.StringResource(R.string.separator_new_messages),
        override val itemId: String
    ) : MessageItem(itemId)

    data class MessageData(
        val id: String,
        val body: String? = null,
        val date: String,
        val time: String,
        val timestamp: LocalDateTime = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()),
        val contactId: String? = null,
        val senderId: String?,
        val senderImage: ContactImage?,
        val senderName: String?,
        val ownerId: String? = null,
        val sentTo: String? = null,
        val replyTo: ReplyingToMessage? = null,
        val status: MessageStatus = MessageStatus.Pending,
        val type: MessageType? = null,
        val read: Boolean = false,
        val attachment: Attachment?,
    ) : MessageItem(id) {
        fun isFromMe() = (senderId ?: ownerId) == ownerId

        fun isOlderThan(message: MessageData): Boolean {
            return this.timestamp.toJavaLocalDateTime().isBefore(message.timestamp.toJavaLocalDateTime())
        }
    }
}

data class ReplyingToMessage(
    val id: String,
    val senderName: String?,
    val body: String?,
    val attachment: Attachment?
)

enum class MessageStatus(val value: Int) {
    Pending(0),
    Error(1),
    Sending(2),
    Sent(3),
    Delivered(4),
    Seen(5);

    companion object {
        fun max(first: MessageStatus, second: MessageStatus) =
            if (first.value >= second.value) first else second
    }
}

enum class MessageType {
    Chat,
    GroupChat
}