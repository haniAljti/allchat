package com.hanialjti.allchat.models

import com.hanialjti.allchat.utils.currentTimestamp
import java.util.*

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val body: String? = null,
    val timestamp: Long = currentTimestamp,
    val conversation: String? = null,
    val from: String? = null,
    val status: String? = null,
    val readBy: Set<String> = setOf(),
    val type: String? = null,
    val media: Media? = null
)
