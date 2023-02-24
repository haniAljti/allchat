package com.hanialjti.allchat.data.remote.xmpp.model

import org.jivesoftware.smack.packet.ExtensionElement
import org.jivesoftware.smack.packet.XmlEnvironment
import org.jivesoftware.smack.util.XmlStringBuilder

data class AvatarDataExtensionElement(
    val data: String
) : ExtensionElement {

    companion object {
        const val NAMESPACE: String = "urn:xmpp:avatar:data"
        const val ELEMENT_NAME: String = "data"
    }

    override fun toXML(xmlEnvironment: XmlEnvironment): CharSequence {
        return XmlStringBuilder(this).apply {
            rightAngleBracket()
            text(data)
            closeElement(elementName)
        }
    }

    override fun getElementName() = ELEMENT_NAME
    override fun getNamespace() = NAMESPACE
}
