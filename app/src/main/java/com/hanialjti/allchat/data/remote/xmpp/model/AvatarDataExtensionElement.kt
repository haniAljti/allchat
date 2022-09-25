package com.hanialjti.allchat.data.remote.xmpp.model

import org.jivesoftware.smack.packet.ExtensionElement
import org.jivesoftware.smack.packet.XmlEnvironment

data class AvatarDataExtensionElement(
    val data: String
) : ExtensionElement {

    companion object {
        const val namespace: String = "urn:xmpp:avatar:data"
        const val elementName: String = "data"
    }

    override fun toXML(xmlEnvironment: XmlEnvironment?): CharSequence {
        return "<$elementName xmlns=$namespace>$data</$elementName>"
    }

    override fun getElementName() = Companion.elementName
    override fun getNamespace() = Companion.namespace
}
