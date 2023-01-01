package com.hanialjti.allchat.data.model

import com.hanialjti.allchat.data.local.room.entity.UserEntity
import com.hanialjti.allchat.presentation.chat.defaultName
import com.hanialjti.allchat.presentation.conversation.ContactImage
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.time.ZoneOffset

data class User(
    val id: String,
    val name: String? = defaultName,
    val image: String? = null,
    val isOnline: Boolean = false,
    val lastOnline: LocalDateTime? = null
)

fun User.toUserEntity() = UserEntity(
    externalId = id,
    name = name,
    image = image,
    isOnline = isOnline,
    lastOnline = lastOnline?.atOffset(ZoneOffset.UTC)
)