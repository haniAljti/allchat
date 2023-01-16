package com.hanialjti.allchat.data.model

sealed class ChatState(open val conversation: String, open val from: String? = null, val state: Participant.State) {
    data class Active(override val conversation: String, override val from: String? = null): ChatState(conversation, from, Participant.State.Active)
    data class Inactive(override val conversation: String, override val from: String? = null): ChatState(conversation, from, Participant.State.Inactive)
    data class Composing(override val conversation: String, override val from: String? = null): ChatState(conversation, from, Participant.State.Composing)
    data class Paused(override val conversation: String, override val from: String? = null): ChatState(conversation, from, Participant.State.Paused)
}