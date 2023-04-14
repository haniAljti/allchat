package com.hanialjti.allchat.data.model

import com.hanialjti.allchat.presentation.conversation.ContactImage


data class Participant(
    val userId: String,
    val state: State,
    val role: Role
) {
    enum class State {
        Active,
        Inactive,
        Composing,
        Paused
    }
}

enum class Role(val value: Int) { None(-1), Participant(0), Admin(1), Owner(2) }

data class ParticipantInfo(
    val id: String,
    val nickname: String?,
    val avatar: ContactImage,
    val state: Participant.State,
    val role: Role
)