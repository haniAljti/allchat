package com.hanialjti.allchat.data.remote.xmpp.model

import org.jivesoftware.smack.packet.ExtensionElement
import org.jivesoftware.smack.packet.XmlEnvironment
import org.jivesoftware.smack.util.XmlStringBuilder

data class AvatarMetaDataExtensionElement(
    val bytes: Int,
    val id: String?,
    val height: Int,
    val width: Int,
    val type: String
) : ExtensionElement {

    constructor(): this(0, null, 0, 0, "")

    companion object {
        const val NAMESPACE: String = "urn:xmpp:avatar:metadata"
        const val ELEMENT_NAME: String = "metadata"
        const val INFO_ELEMENT: String = "info"

        const val ATTR_ID = "id"
        const val ATTR_BYTES = "bytes"
        const val ATTR_HEIGHT = "height"
        const val ATTR_WIDTH = "wight"
        const val ATTR_TYPE = "type"
    }

    override fun toXML(xmlEnvironment: XmlEnvironment): CharSequence {
        return if (id == null) {
            XmlStringBuilder(this).closeEmptyElement() //this disables avatar publishing
        }
        else XmlStringBuilder(this).apply {
            rightAngleBracket()
            halfOpenElement(INFO_ELEMENT)
            attribute(ATTR_ID, id)
            attribute(ATTR_BYTES, bytes)
            attribute(ATTR_HEIGHT, height)
            attribute(ATTR_WIDTH, width)
            attribute(ATTR_TYPE, type)
            closeEmptyElement()
            closeElement(elementName)
        }
    }

    override fun getElementName() = Companion.ELEMENT_NAME
    override fun getNamespace() = Companion.NAMESPACE
}
