package com.hanialjti.allchat.data.remote.xmpp.model

import com.hanialjti.allchat.data.model.Avatar
import org.jivesoftware.smack.packet.IQ

data class VCard(
    val nickname: String,
    val avatar: Avatar?
) : IQ(ELEMENT, NAMESPACE) {

    companion object {
        const val ELEMENT = "vCard"
        const val NAMESPACE = "vcard-temp"
        private const val NICKNAME = "NICKNAME"
    }

    override fun getIQChildElementBuilder(xml: IQChildElementXmlStringBuilder): IQChildElementXmlStringBuilder {
        return xml.apply {
            rightAngleBracket()
            if (avatar != null) {
                openElement("PHOTO")
//                when (avatar) {
//                    is Avatar.Raw -> {
//                        escapedElement("BINVAL", avatar.bytes)
//                    }
//                    is Avatar.Url -> {
//                        escapedElement("EXTVAL", avatar.data)
//                    }
//                }
//                element("TYPE", avatar.mimeType)
                closeElement("PHOTO")
            }
            optElement(NICKNAME, nickname)
        }
    }
}
