package com.hanialjti.allchat.data.remote.model


data class MessagePage(
    val messageList: List<RemoteMessageItem> = listOf(),
    val isComplete: Boolean,
    val error: Throwable? = null
)
