package com.hanialjti.allchat.data.remote.xmpp.model

import org.jivesoftware.smack.packet.ExtensionElement
import org.jivesoftware.smack.packet.XmlEnvironment
import org.jivesoftware.smack.provider.EmbeddedExtensionProvider
import org.jivesoftware.smack.util.XmlStringBuilder
import org.jivesoftware.smackx.nick.packet.Nick

data class MucBookmark(
    val name: String? = null,
    val autoJoin: Boolean = true,
    val nick: String? = null
) : ExtensionElement {

    companion object {
        const val NAMESPACE = "urn:xmpp:bookmarks:1"
        const val ELEMENT = "conference"

        private const val NAME_ATTRIBUTE = "name"
        private const val AUTO_JOIN_ATTRIBUTE = "autojoin"
    }

    override fun toXML(xmlEnvironment: XmlEnvironment?): CharSequence {
        return XmlStringBuilder(this).apply {
            optAttribute(NAME_ATTRIBUTE, name)
            attribute(AUTO_JOIN_ATTRIBUTE, autoJoin)
            rightAngleBracket()
            nick?.let { append(Nick(nick)) }
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

class MucBookmarkExtensionProvider: EmbeddedExtensionProvider<MucBookmark>() {
    override fun createReturnExtension(
        currentElement: String?,
        currentNamespace: String?,
        attributeMap: MutableMap<String, String>?,
        content: MutableList<out ExtensionElement>?
    ): MucBookmark {
        val nick = content?.getOrNull(0) as? Nick
        return MucBookmark(
            name = attributeMap?.getOrDefault("name", null),
            autoJoin = attributeMap?.getOrDefault("autojoin", "true").toBoolean(),
            nick = nick?.name
        )
    }
}
