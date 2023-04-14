package com.hanialjti.allchat.data.local.room.model

import com.hanialjti.allchat.data.model.UserDetails
import com.hanialjti.allchat.presentation.conversation.ContactImage

data class UserDetailsEntity(
    val id: String,
    val avatar: String?,
    val nickname: String?,
    val isOnline: Boolean = false,
    val status: String? = null,
    val isBlocked: Boolean = false,
)

fun UserDetailsEntity.asUserDetails() = UserDetails(
    id = id,
    name = nickname,
    avatar = avatar?.let { ContactImage.DynamicImage(it) }
        ?: ContactImage.DefaultProfileImage(false),
    status = status,
    isOnline = isOnline,
    isBlocked = isBlocked
)
