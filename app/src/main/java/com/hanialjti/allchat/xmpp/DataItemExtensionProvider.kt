package com.hanialjti.allchat.xmpp

import org.jivesoftware.smack.packet.XmlEnvironment
import org.jivesoftware.smack.provider.ExtensionElementProvider
import org.jivesoftware.smack.xml.XmlPullParser

class DataItemExtensionProvider: ExtensionElementProvider<DataExtensionElement>() {

    override fun parse(
        parser: XmlPullParser?,
        initialDepth: Int,
        xmlEnvironment: XmlEnvironment?
    ): DataExtensionElement {
        val data = parser?.nextText()

        return DataExtensionElement(data ?: "")
    }

}