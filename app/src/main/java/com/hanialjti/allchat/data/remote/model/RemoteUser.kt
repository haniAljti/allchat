package com.hanialjti.allchat.data.remote.model

import com.hanialjti.allchat.data.model.Avatar
import java.time.OffsetDateTime


data class FullRemoteUserInfo(
    val id: String,
    val name: String?,
    val avatar: Avatar?,
    val isOnline: Boolean,
    val status: String?,
    val lastOnline: OffsetDateTime? = null
)