package com.hanialjti.allchat.data.model

import com.hanialjti.allchat.presentation.conversation.ContactContent
import com.hanialjti.allchat.presentation.conversation.ContactImage
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

data class ContactWithLastMessage(
    val id: String? = null,
    val lastMessage: MessageSummary? = null,
    val isGroupChat: Boolean = false,
    val composing: List<String> = listOf(),
    val name: String? = null,
    val image: ContactImage? = null,
    val isOnline: Boolean = false,
    val content: ContactContent? = null,
    val to: User? = null,
    val unreadMessages: Int = 0,
)

@Serializable
data class MessageSummary(
    val body: String?,
    val status: MessageStatus,
    val timestamp: LocalDateTime,
    val attachmentType: Attachment.Type?,
    val isSentByMe: Boolean = false
)