package com.hanialjti.allchat.data.remote.xmpp

import com.hanialjti.allchat.data.remote.xmpp.model.AvatarDataExtensionElement
import org.jivesoftware.smack.packet.XmlEnvironment
import org.jivesoftware.smack.provider.ExtensionElementProvider
import org.jivesoftware.smack.xml.XmlPullParser

class DataItemExtensionProvider: ExtensionElementProvider<AvatarDataExtensionElement>() {

    override fun parse(
        parser: XmlPullParser?,
        initialDepth: Int,
        xmlEnvironment: XmlEnvironment?
    ): AvatarDataExtensionElement {
        val data = parser?.nextText()

        return AvatarDataExtensionElement(data ?: "")
    }

}