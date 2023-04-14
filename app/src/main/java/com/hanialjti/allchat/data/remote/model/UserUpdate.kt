package com.hanialjti.allchat.data.remote.model

import com.hanialjti.allchat.data.model.Avatar
import java.time.OffsetDateTime

sealed class UserUpdate(val userId: String)

class PresenceUpdate(
    userId: String,
    val isOnline: Boolean,
    val status: String?,
    val lastOnline: OffsetDateTime?
) : UserUpdate(userId)

class NicknameUpdated(userId: String, val nickname: String) : UserUpdate(userId)
class AvatarMetadataUpdated(
    userId: String,
    val size: Int,
    val hash: String?,
    val height: Int,
    val width: Int,
    val url: String?,
    val type: String
) : UserUpdate(userId)

class AvatarUpdated(userId: String, val avatar: Avatar?) : UserUpdate(userId)