package com.hanialjti.allchat.data.remote.xmpp.model

import org.jivesoftware.smackx.vcardtemp.packet.VCard

data class VCardWrapper(
    val vCard: VCard
) {
    val name: String = vCard.nickName
    val image: ByteArray = vCard.avatar
}
