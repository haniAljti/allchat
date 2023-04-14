package com.hanialjti.allchat.data.remote.model

import java.time.OffsetDateTime

data class RemoteRoomInfo(
    val description: String?,
    val avatarUrl: String?,
    val createdAt: OffsetDateTime?,
    val createdBy: String?,
    val participants: Map<String, RemoteParticipant>
)
