package com.hanialjti.allchat.data.model


data class Participant(
    val id: String,
    val name: String,
    val image: String,
    val state: Contact.State,
    val roles: List<Role>
)

enum class Role(val value: Int) { Participant(0), Owner(1), Admin(2) }