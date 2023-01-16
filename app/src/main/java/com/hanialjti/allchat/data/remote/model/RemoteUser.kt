package com.hanialjti.allchat.data.remote.model

import com.hanialjti.allchat.data.model.Avatar
import java.time.OffsetDateTime


sealed interface RemoteUserItem

data class RemoteUser(
    val id: String,
    val name: String?,
    val avatar: Avatar?
) : RemoteUserItem

data class RemotePresence(
    val id: String,
    val isOnline: Boolean,
    val status: String?,
    val lastOnline: OffsetDateTime? = null
) : RemoteUserItem

data class FullUserInfo(
    val user: RemoteUser,
    val presence: RemotePresence
)

sealed interface UserUpdate {
    class NameUpdate(val userId: String, val name: String) : UserUpdate
    class UrlImageUpdate(val userId: String, val imageUrl: String) : UserUpdate
    class BinaryImageUpdate(val userId: String, val imageInfo: ImageInfo) : UserUpdate
}

data class ImageInfo(
    val sizeInBytes: Int,
    val height: Int,
    val width: Int,
    val mimeType: String?,
    val hash: String
)