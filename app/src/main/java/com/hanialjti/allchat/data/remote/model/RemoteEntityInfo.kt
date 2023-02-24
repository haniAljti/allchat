package com.hanialjti.allchat.data.remote.model

import com.hanialjti.allchat.data.model.Avatar

data class RemoteEntityInfo(
    val id: String,
    val name: String?,
    val avatar: Avatar?
)
