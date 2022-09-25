package com.hanialjti.allchat.data.remote.xmpp.model

import com.hanialjti.allchat.data.local.room.entity.Status

sealed class ChatMarker(val stanzaId: String) {
    class Received(stanzaId: String): ChatMarker(stanzaId)
    class Displayed(stanzaId: String): ChatMarker(stanzaId)
    class Acknowledged(stanzaId: String): ChatMarker(stanzaId)

    companion object {
        fun ChatMarker.toStatus() = when (this) {
            is Received -> Status.Received
            is Displayed -> Status.Seen
            is Acknowledged -> Status.Acknowledged
        }
    }

}

