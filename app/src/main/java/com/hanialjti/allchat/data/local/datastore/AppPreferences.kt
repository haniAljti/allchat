package com.hanialjti.allchat.data.local.datastore

import kotlinx.serialization.Serializable

@Serializable
data class AppPreferences(
    val userCredentials: UserCredentials? = UserCredentials(),
    val clientPreferences: ClientPreferences = ClientPreferences(),
    val loggedInUser: String? = null,
    val isSynced: Boolean = false
)

@Serializable
data class UserCredentials(
    val username: String? = null,
    val password: String? = null
)

@Serializable
data class ClientPreferences(
    val enableChatMarkers: Boolean = true,
    val enableChatStateNotifications: Boolean = true,
    val enablePresenceSending: Boolean = true,
    val presenceStatus: String? = null,
    val sendPresenceOnLogin: Boolean = enablePresenceSending,
)

@Serializable
data class LoggedInUser(
    val id: String,
    val name: String? = null,
    val image: String? = null,
)