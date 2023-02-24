package com.hanialjti.allchat.data.remote.xmpp.model

import org.jivesoftware.smack.packet.ExtensionElement
import org.jivesoftware.smack.packet.XmlEnvironment
import org.jivesoftware.smack.util.XmlStringBuilder

data class MucBookmark(
    val name: String,
    val autoJoin: Boolean
) : ExtensionElement {

    companion object {
        const val NAMESPACE = "urn:xmpp:bookmarks:1"
        const val ELEMENT = "conference"

        private const val NAME_ATTRIBUTE = "name"
        private const val AUTO_JOIN_ATTRIBUTE = "autojoin"
    }

    override fun toXML(xmlEnvironment: XmlEnvironment?): CharSequence {
        return XmlStringBuilder(this).apply {
            attribute(NAME_ATTRIBUTE, name)
            attribute(AUTO_JOIN_ATTRIBUTE, autoJoin)
            rightAngleBracket()
//            openElement(NICK_ATTRIBUTE).text(nick).closeElement(NICK_ATTRIBUTE)
            closeElement(ELEMENT)
        }
    }

    override fun getElementName(): String {
        return ELEMENT
    }

    override fun getNamespace(): String {
        return NAMESPACE
    }
}
