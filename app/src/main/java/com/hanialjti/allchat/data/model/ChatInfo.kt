package com.hanialjti.allchat.data.model

import com.hanialjti.allchat.presentation.conversation.ContactImage

data class ChatInfo(
    val id: String? = null,
    val isGroupChat: Boolean = false,
    val name: String? = null,
    val image: ContactImage? = null,
    val isOnline: Boolean = false,
    val description: String? = null,
    val participants: Set<Participant> = setOf()
)
