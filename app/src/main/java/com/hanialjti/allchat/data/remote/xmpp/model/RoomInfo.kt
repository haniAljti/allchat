package com.hanialjti.allchat.data.remote.xmpp.model

import com.hanialjti.allchat.data.remote.model.RemoteParticipant
import kotlinx.serialization.Serializable

@Serializable
data class RoomInfo(
    val description: String?,
    val createdAt: String?,
    val createdBy: String?,
    val avatarUrl: String?
)