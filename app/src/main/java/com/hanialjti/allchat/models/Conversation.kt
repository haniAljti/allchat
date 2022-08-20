package com.hanialjti.allchat.models

import com.hanialjti.allchat.utils.currentTimestamp

data class Conversation(
    var id: String,
    var lastMessage: String? = null,
    var lastUpdated: Long = currentTimestamp,
    var isGroupChat: Boolean = false,
    var name: String? = null,
    var imageUrl: String? = null,
    var ownerId: String? = null,
    var members: MutableList<String>? = mutableListOf(),
    var unreadMessages: Int = 0,
)
