package com.hanialjti.allchat.data.remote.xmpp.model

import org.jivesoftware.smack.packet.ExtensionElement
import org.jivesoftware.smack.packet.XmlEnvironment
import org.jivesoftware.smack.util.XmlStringBuilder
import javax.xml.namespace.QName

data class OutOfBandData(
    val url: String,
    val desc: String?
) : ExtensionElement {

    companion object {
        const val NAMESPACE = "jabber:x:oob"
        const val ELEMENT = "x"

        private const val URL_ATTRIBUTE = "url"
        private const val DESC_ATTRIBUTE = "desc"
    }

    override fun toXML(xmlEnvironment: XmlEnvironment?): CharSequence {
        return XmlStringBuilder(this).apply {
            rightAngleBracket()
            element(URL_ATTRIBUTE, url)
            desc?.let { element(DESC_ATTRIBUTE, desc) }
            closeElement(ELEMENT)
        }
    }

    override fun getElementName(): String {
        return ELEMENT
    }

    override fun getNamespace(): String {
        return NAMESPACE
    }

    override fun getQName() = QName(NAMESPACE, ELEMENT)
}
