package com.hanialjti.allchat.data.local.datastore

import kotlinx.serialization.Serializable

@Serializable
data class UserPreferences(
    val userCredentials: UserCredentials? = UserCredentials(),
    val loggedInUser: LoggedInUser? = null
)

@Serializable
data class UserCredentials(
    val username: String? = null,
    val password: String? = null
)

@Serializable
data class ClientPreferences(
    val enableChatMarkers: Boolean = true,
)

@Serializable
data class LoggedInUser(
    val id: String,
    val name: String? = null,
    val image: String? = null,
)