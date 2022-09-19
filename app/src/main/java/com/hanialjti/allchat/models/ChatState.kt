package com.hanialjti.allchat.models

import com.hanialjti.allchat.models.entity.State

sealed class ChatState(open val conversation: String, open val from: String? = null, val state: State) {
    data class Active(override val conversation: String, override val from: String? = null): ChatState(conversation, from, State.Active)
    data class Inactive(override val conversation: String, override val from: String? = null): ChatState(conversation, from, State.Inactive)
    data class Composing(override val conversation: String, override val from: String? = null): ChatState(conversation, from, State.Composing)
    data class Paused(override val conversation: String, override val from: String? = null): ChatState(conversation, from, State.Paused)
}