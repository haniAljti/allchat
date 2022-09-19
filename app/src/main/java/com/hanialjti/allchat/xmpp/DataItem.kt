package com.hanialjti.allchat.xmpp

import org.jivesoftware.smack.packet.ExtensionElement
import org.jivesoftware.smack.packet.XmlEnvironment

data class DataExtensionElement(
    val data: String
) : ExtensionElement {

    companion object {
        const val namespace: String = "urn:xmpp:avatar:data"
        const val elementName: String = "data"
    }

    override fun toXML(xmlEnvironment: XmlEnvironment?): CharSequence {
        return "<$elementName xmlns=$namespace>$data</$elementName>"
    }

    override fun getElementName() = DataExtensionElement.elementName
    override fun getNamespace() = DataExtensionElement.namespace
}
