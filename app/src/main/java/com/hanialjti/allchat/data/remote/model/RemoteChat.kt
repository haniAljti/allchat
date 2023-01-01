package com.hanialjti.allchat.data.remote.model

data class RemoteChat(
    val id: String,
    val name: String?,
    val image: String? = null,
    val isGroupChat: Boolean
)