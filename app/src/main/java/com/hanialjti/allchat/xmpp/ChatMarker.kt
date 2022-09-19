package com.hanialjti.allchat.xmpp

sealed class ChatMarker(val stanzaId: String) {
    class Received(stanzaId: String): ChatMarker(stanzaId)
    class Displayed(stanzaId: String): ChatMarker(stanzaId)
    class Acknowledged(stanzaId: String): ChatMarker(stanzaId)
}
