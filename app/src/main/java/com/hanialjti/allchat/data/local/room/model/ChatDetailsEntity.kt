package com.hanialjti.allchat.data.local.room.model

import com.hanialjti.allchat.data.model.ChatDetails
import com.hanialjti.allchat.presentation.conversation.ContactImage
import java.time.OffsetDateTime

data class ChatDetailsEntity(
    val id: String,
    val isGroupChat: Boolean,
    val avatar: String?,
    val nickname: String?,
    val description: String?,
    val createdAt: OffsetDateTime?
)

fun ChatDetailsEntity.asChatDetails() = ChatDetails(
    id = id,
    isGroupChat = isGroupChat,
    name = nickname,
    avatar = avatar?.let { ContactImage.DynamicImage(it) }
        ?: ContactImage.DefaultProfileImage(isGroupChat),
    description = description,
    createdBy = null,
    createdAt = createdAt?.toLocalDateTime()
)
