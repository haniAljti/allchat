package com.hanialjti.allchat.xmpp

import com.hanialjti.allchat.models.entity.Status

sealed class ChatMarker(val stanzaId: String) {
    class Received(stanzaId: String): ChatMarker(stanzaId)
    class Displayed(stanzaId: String): ChatMarker(stanzaId)
    class Acknowledged(stanzaId: String): ChatMarker(stanzaId)
}

fun ChatMarker.toStatus() = when (this) {
    is ChatMarker.Received -> Status.Received
    is ChatMarker.Displayed -> Status.Seen
    is ChatMarker.Acknowledged -> Status.Acknowledged
}