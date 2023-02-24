package com.hanialjti.allchat.data.remote.xmpp.model

import org.jivesoftware.smack.packet.ExtensionElement
import org.jivesoftware.smack.packet.StandardExtensionElement
import org.jivesoftware.smack.provider.EmbeddedExtensionProvider

class OOBExtensionProvider: EmbeddedExtensionProvider<OutOfBandData>() {
    override fun createReturnExtension(
        currentElement: String?,
        currentNamespace: String?,
        attributeMap: MutableMap<String, String>?,
        content: MutableList<out ExtensionElement>?
    ): OutOfBandData {
        val urlElement = content?.getOrNull(0) as? StandardExtensionElement
        val descElement = content?.getOrNull(1) as? StandardExtensionElement
        return OutOfBandData(
            url = urlElement?.text ?: "",
            desc = descElement?.text
        )
    }
}