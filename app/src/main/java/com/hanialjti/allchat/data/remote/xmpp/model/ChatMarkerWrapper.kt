package com.hanialjti.allchat.data.remote.xmpp.model

import com.hanialjti.allchat.data.model.Marker
import com.hanialjti.allchat.data.model.MessageStatus

sealed class ChatMarkerWrapper(val stanzaId: String) {
    class Delivered(stanzaId: String): ChatMarkerWrapper(stanzaId)
    class Displayed(stanzaId: String): ChatMarkerWrapper(stanzaId)
    class Acknowledged(stanzaId: String): ChatMarkerWrapper(stanzaId)

    companion object {
        fun ChatMarkerWrapper.toMarker() = when (this) {
            is Delivered -> Marker.Delivered
            else -> Marker.Seen
        }

        fun ChatMarkerWrapper.toMessageStatus() = when (this) {
            is Delivered -> MessageStatus.Delivered
            else -> MessageStatus.Seen
        }
    }

}

