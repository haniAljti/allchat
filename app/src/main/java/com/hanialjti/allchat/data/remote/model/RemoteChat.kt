package com.hanialjti.allchat.data.remote.model

import com.hanialjti.allchat.data.model.Avatar

data class RemoteChat(
    val id: String,
    val name: String?,
    val avatar: Avatar? = null,
    val isGroupChat: Boolean
)