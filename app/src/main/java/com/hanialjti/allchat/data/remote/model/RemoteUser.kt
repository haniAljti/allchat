package com.hanialjti.allchat.data.remote.model

import com.hanialjti.allchat.data.local.room.entity.UserEntity
import java.time.OffsetDateTime

data class RemoteUser(
    val id: String,
    val name: String?,
    val image: String?
)

sealed interface RemoteUserItem {
    data class UserData(
        val id: String,
        val name: String?,
        val image: String?
    ): RemoteUserItem

    data class UserPresence(
        val id: String,
        val isOnline: Boolean,
        val status: String?,
        val lastOnline: OffsetDateTime? = null
    ): RemoteUserItem
}

fun RemoteUserItem.UserData.toUserEntity() = UserEntity(
    externalId = id,
    name = name,
    image = image
)