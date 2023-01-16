package com.hanialjti.allchat.data.model


data class Participant(
    val id: String,
    val name: String,
    val image: String,
    val state: State,
    val roles: Role
) {
    enum class State {
        Active,
        Inactive,
        Composing,
        Paused
    }
}

enum class Role(val value: Int) { Participant(0), Admin(1) }
