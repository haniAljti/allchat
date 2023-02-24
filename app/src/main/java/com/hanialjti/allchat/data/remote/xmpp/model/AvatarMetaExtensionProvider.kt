package com.hanialjti.allchat.data.remote.xmpp.model

import org.jivesoftware.smack.packet.ExtensionElement
import org.jivesoftware.smack.packet.StandardExtensionElement
import org.jivesoftware.smack.packet.XmlEnvironment
import org.jivesoftware.smack.provider.EmbeddedExtensionProvider
import org.jivesoftware.smack.provider.ExtensionElementProvider
import org.jivesoftware.smack.xml.XmlPullParser

class AvatarMetaDataExtensionProvider: EmbeddedExtensionProvider<AvatarMetaDataExtensionElement>() {
    override fun createReturnExtension(
        currentElement: String?,
        currentNamespace: String?,
        attributeMap: MutableMap<String, String>?,
        content: MutableList<out ExtensionElement>?
    ): AvatarMetaDataExtensionElement {
        val info = content?.getOrNull(0) as? StandardExtensionElement
        val attributes = info?.attributes
        return AvatarMetaDataExtensionElement(
            bytes = attributes?.get(AvatarMetaDataExtensionElement.ATTR_BYTES)?.toInt() ?: 0,
            id = attributes?.get(AvatarMetaDataExtensionElement.ATTR_ID) ?: "",
            height = attributes?.get(AvatarMetaDataExtensionElement.ATTR_HEIGHT)?.toInt() ?: 96,
            width = attributes?.get(AvatarMetaDataExtensionElement.ATTR_WIDTH)?.toInt() ?: 96,
            type = attributes?.get(AvatarMetaDataExtensionElement.ATTR_TYPE) ?: "image/png",
        )
    }
}

class AvatarExtensionProvider: ExtensionElementProvider<AvatarDataExtensionElement>() {

    override fun parse(
        parser: XmlPullParser?,
        initialDepth: Int,
        xmlEnvironment: XmlEnvironment?
    ): AvatarDataExtensionElement {
        parser?.next()
        val data = parser?.text
        return AvatarDataExtensionElement(
            data = data ?: "",
        )
    }
}