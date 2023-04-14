package com.hanialjti.allchat.data.remote.model

import com.hanialjti.allchat.common.utils.KOffsetDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

@Serializable
data class RoomState(
    val id: String,
    val description: String? = null,
    @Serializable(with = KOffsetDateTimeSerializer::class)
    val createdAt: OffsetDateTime? = null,
    val createdBy: String? = null,
    val avatarUrl: String? = null,
    val subject: String? = null,
    val participants: Map<String, RemoteParticipant> = mapOf()
)
