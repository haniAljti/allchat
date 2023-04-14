package com.hanialjti.allchat.data.model

import com.hanialjti.allchat.presentation.conversation.ContactImage
import java.time.LocalDateTime

data class ChatDetails(
    val id: String? = null,
    val isGroupChat: Boolean = false,
    val name: String? = null,
    val avatar: ContactImage,
    val createdBy: ParticipantInfo?,
    val createdAt: LocalDateTime? = null,
    val description: String? = null,
    val participants: Set<ParticipantInfo> = setOf()
)

data class UserDetails(
    val id: String? = null,
    val name: String? = null,
    val avatar: ContactImage,
    val isOnline: Boolean = false,
    val status: String? = null,
    val isBlocked: Boolean = false
)
