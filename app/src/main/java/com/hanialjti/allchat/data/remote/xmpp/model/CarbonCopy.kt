package com.hanialjti.allchat.data.remote.xmpp.model

import com.hanialjti.allchat.data.remote.xmpp.wrapMarker
import org.jivesoftware.smack.packet.Message

class CarbonCopy(message: Message) {

    private val id: String

    init {
        val marker = message.wrapMarker()
        id = marker?.stanzaId ?: ""
    }
}