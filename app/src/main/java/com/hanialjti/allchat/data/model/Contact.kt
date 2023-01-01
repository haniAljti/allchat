package com.hanialjti.allchat.data.model

import com.hanialjti.allchat.presentation.conversation.ContactContent
import com.hanialjti.allchat.presentation.conversation.ContactImage

data class Contact(
    val id: String? = null,
    val lastMessage: MessageSummary? = null,
    val isGroupChat: Boolean = false,
    val composing: List<String> = listOf(),
    val name: String? = null,
    val image: ContactImage? = null,
    val isOnline: Boolean = false,
    val content: ContactContent? = null,
    val to: User? = null,
    val unreadMessages: Int = 0,
) {

    enum class State {
        Active,
        Inactive,
        Composing,
        Paused
    }
}