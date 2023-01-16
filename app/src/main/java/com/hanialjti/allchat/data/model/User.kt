package com.hanialjti.allchat.data.model

import com.hanialjti.allchat.presentation.chat.defaultName
import com.hanialjti.allchat.presentation.conversation.ContactImage
import java.time.LocalDateTime

data class User(
    val id: String,
    val name: String? = defaultName,
    val avatar: ContactImage? = null,
    val isOnline: Boolean = false,
    val lastOnline: LocalDateTime? = null
)