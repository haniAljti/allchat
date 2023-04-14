package com.hanialjti.allchat.data.remote.model

import com.hanialjti.allchat.data.model.Role
import kotlinx.serialization.Serializable

@Serializable
data class RemoteParticipant(
    val id: String,
    val role: Role
)
