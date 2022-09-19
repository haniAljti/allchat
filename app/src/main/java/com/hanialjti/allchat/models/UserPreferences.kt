package com.hanialjti.allchat.models

import kotlinx.serialization.Serializable

@Serializable
data class UserPreferences(
    val userCredentials: UserCredentials? = null
)

@Serializable
data class UserCredentials(
    val username: String? = null,
    val password: String? = null
)
