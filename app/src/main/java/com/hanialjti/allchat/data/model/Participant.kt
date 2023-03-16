package com.hanialjti.allchat.data.model


data class Participant(
    val user: User,
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

enum class Role(val value: Int) { Participant(0), Admin(1) }
