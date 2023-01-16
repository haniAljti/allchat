package com.hanialjti.allchat.data.remote.xmpp

import com.hanialjti.allchat.data.local.room.entity.MessageEntity
import com.hanialjti.allchat.data.model.MessageStatus
import com.hanialjti.allchat.data.model.MessageType
import com.hanialjti.allchat.data.remote.model.RemoteMessage
import com.hanialjti.allchat.data.remote.xmpp.model.ChatMarkerWrapper
import com.hanialjti.allchat.data.remote.xmpp.model.VCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jivesoftware.smack.SmackException.NoResponseException
import org.jivesoftware.smack.SmackException.NotConnectedException
import org.jivesoftware.smack.XMPPException.XMPPErrorException
import org.jivesoftware.smack.packet.IQ
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.packet.Stanza
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smackx.chat_markers.element.ChatMarkersElements
import org.jivesoftware.smackx.chatstates.ChatState
import org.jivesoftware.smackx.chatstates.packet.ChatStateExtension
import org.jivesoftware.smackx.forward.packet.Forwarded
import org.jivesoftware.smackx.muc.MultiUserChat
import org.jivesoftware.smackx.muc.packet.GroupChatInvitation
import org.jivesoftware.smackx.vcardtemp.VCardManager
import org.jxmpp.jid.BareJid
import org.jxmpp.jid.EntityBareJid
import org.jxmpp.jid.Jid
import org.jxmpp.jid.impl.JidCreate


fun Message.Type.toMessageType() =
    if (this == Message.Type.groupchat) MessageType.GroupChat else MessageType.Chat

fun Jid.isGroupConversation() = this.asDomainBareJid().contains("@conference.")

suspend fun String.asJid(): BareJid =
    withContext(Dispatchers.IO) { return@withContext JidCreate.bareFrom(this@asJid) }

fun Stanza.getStringFrom() = from.localpartOrNull?.toString()

fun MessageType.toXmppMessageType() = when (this) {
    MessageType.Chat -> Message.Type.chat
    MessageType.GroupChat -> Message.Type.groupchat
}

fun MessageEntity.toMessageStanza(connection: XMPPTCPConnection): Message =
    connection.stanzaFactory
        .buildMessageStanza()
        .to(contactId)
        .ofType(Message.Type.chat)
        .setBody(body)
        .addExtension(ChatMarkersElements.MarkableExtension.INSTANCE)
        .build()

fun ChatState.toConversationState(conversation: String, from: String) = when (this) {
    ChatState.active -> com.hanialjti.allchat.data.model.ChatState.Active(
        conversation,
        from
    )
    ChatState.composing -> com.hanialjti.allchat.data.model.ChatState.Composing(
        conversation,
        from
    )
    ChatState.inactive -> com.hanialjti.allchat.data.model.ChatState.Inactive(
        conversation,
        from
    )
    else -> {
        com.hanialjti.allchat.data.model.ChatState.Paused(conversation, from)
    }
}

//fun NetworkMessage.toStanzaMessage(
//    connection: XMPPTCPConnection,
//    isMarkable: Boolean
//) = connection.stanzaFactory
//    .buildMessageStanza()
//    .to(contactId)
//    .from(connection.user?.asBareJid()?.toString())
//    .ofType(type?.toXmppMessageType())
//    .setBody(body)
//    .apply {
//        if (isMarkable) {
//            addExtension(ChatMarkersElements.MarkableExtension.INSTANCE)
//        }
//    }
//    .build()

fun MessageEntity.toStanzaMessage(
    connection: XMPPTCPConnection,
    isMarkable: Boolean
) = connection.stanzaFactory
    .buildMessageStanza()
    .to(contactId)
    .from(connection.user?.asBareJid()?.toString())
    .ofType(type?.toXmppMessageType())
    .setBody(body)
    .apply {
        if (isMarkable) {
            addExtension(ChatMarkersElements.MarkableExtension.INSTANCE)
        }
    }
    .build()


fun XMPPTCPConnection.getOwner() = user?.asBareJid()?.toString()

fun Message.wrapMarker(): ChatMarkerWrapper? = when {
    hasExtension(ChatMarkersElements.DisplayedExtension.QNAME) -> {
        val displayedMarker = getExtension(ChatMarkersElements.DisplayedExtension.QNAME)
                as ChatMarkersElements.DisplayedExtension
        ChatMarkerWrapper.Displayed(displayedMarker.id)
    }
    hasExtension(ChatMarkersElements.AcknowledgedExtension.QNAME) -> {
        val acknowledgedMarker = getExtension(ChatMarkersElements.AcknowledgedExtension.QNAME)
                as ChatMarkersElements.AcknowledgedExtension
        ChatMarkerWrapper.Acknowledged(acknowledgedMarker.id)
    }
    hasExtension(ChatMarkersElements.ReceivedExtension.QNAME) -> {
        val receivedMarker = getExtension(ChatMarkersElements.ReceivedExtension.QNAME)
                as ChatMarkersElements.ReceivedExtension
        ChatMarkerWrapper.Delivered(receivedMarker.id)
    }
    else -> null
}

fun Stanza.toAckMessage() = RemoteMessage(
    id = stanzaId,
    chatId = from?.asBareJid()?.toString(),
    body = if (this is Message) body else null,
    sender = from?.asBareJid()?.toString(),
    type = getType(),
    thread = null,
    messageStatus = MessageStatus.Sent,
)

fun Stanza.isMessage() = this is Message
fun Stanza.isChatState() = this.hasExtension(ChatStateExtension.NAMESPACE)

fun Stanza.isMucInvitation() = this.hasExtension(GroupChatInvitation.NAMESPACE)

fun Stanza.fromAsString() = this.from?.asBareJid()?.toString()
fun Stanza.toAsString() = this.to?.asBareJid()?.toString()
fun Stanza.fromAsResourceString() = from?.resourceOrNull?.toString()

//fun Stanza.toMessage(connection: XMPPTCPConnection) = MessageEntity(
//    externalId = stanzaId,
//    body = if (this is Message) body else null,
//    contactId = connection.getConversationIdFromStanza(this),
//    senderId = from?.asBareJid()?.toString(),
//    ownerId = connection.getOwner(),
//    type = getType()
//)
//
//fun Stanza.toGroupChatMessage(connection: XMPPTCPConnection) = MessageEntity(
//    externalId = stanzaId,
//    body = if (this is Message) body else null,
//    contactId = from?.asBareJid()?.toString(),
//    senderId = from?.resourceOrNull?.toString(),
//    ownerId = connection.getOwner(),
//    type = getType()
//)

fun Stanza.getType() = if (this is Message) {
    type.toMessageType()
} else null

fun Stanza.isMessageAck(): Boolean =
    this is Message && type != Message.Type.error && to != null && stanzaId != null

fun Forwarded<Message>.timestamp() = delayInformation.stamp.time

fun XMPPTCPConnection.getConversationIdFromStanza(stanza: Stanza): String? {
    return if (stanza is Message) {
        if (stanza.type == Message.Type.groupchat)
            stanza.to.asBareJid().toString()
        else {
            if (stanza.from?.asBareJid()?.toString() == getOwner()) {
                stanza.to?.asBareJid()?.toString()
            } else {
                stanza.from?.asBareJid()?.toString()
            }
        }
    } else {
        stanza.to.asBareJid().toString()
    }
}

/**
 * Invites another user to the room in which one is an occupant. In contrast
 * to the method "invite", the invitation is sent directly to the user rather
 * than via the chat room.  This is useful when the user being invited is
 * offline, as otherwise the invitation would be dropped.
 *
 * @param user the user to send the invitation to
 * @throws NotConnectedException if the XMPP connection is not connected.
 * @throws InterruptedException if the calling thread was interrupted.
 */
@Throws(NotConnectedException::class, InterruptedException::class)
fun MultiUserChat.inviteDirectly(user: EntityBareJid?) {
//    MessageBuilder.buildMessage(UUID.randomUUID().toString())
    val messageBuilder = xmppConnection.stanzaFactory.buildMessageStanza()
    messageBuilder.to(user)

    // Add the extension for direct invitation
    val invitationExt = GroupChatInvitation(room.toString())
    messageBuilder.apply {
        addExtension(invitationExt)
    }

    // Send it
    val message = messageBuilder.build()
    xmppConnection.sendStanza(message)
}

/**
 * @param roomAddress only pass a roomAddress in case you want to set as a MUC VCard
 */
suspend fun VCardManager.updateVCard(vcard: VCard, connection: XMPPTCPConnection, roomAddress: String? = null) {
    // XEP-54 § 3.2 "A user may publish or update his or her vCard by sending an IQ of type "set" with no 'to' address…"

    // XEP-54 § 3.2 "A user may publish or update his or her vCard by sending an IQ of type "set" with no 'to' address…"
    vcard.to = roomAddress?.asJid()
    vcard.type = IQ.Type.set

    // Also make sure to generate a new stanza id (the given vcard could be a vcard result), in which case we don't
    // want to use the same stanza id again (although it wouldn't break if we did)
    // Also make sure to generate a new stanza id (the given vcard could be a vcard result), in which case we don't
    // want to use the same stanza id again (although it wouldn't break if we did)
    vcard.setStanzaId()
    connection.createStanzaCollectorAndSend(vcard).nextResultOrThrow<Stanza>()
}

@Throws(
    NoResponseException::class,
    XMPPErrorException::class,
    NotConnectedException::class,
    InterruptedException::class
)
fun VCardManager.getVCard(bareJid: EntityBareJid?, connection: XMPPTCPConnection): VCard {
    val vcardRequest =
        org.jivesoftware.smackx.vcardtemp.packet.VCard()
    vcardRequest.to = bareJid
    return connection.createStanzaCollectorAndSend(vcardRequest)
        .nextResultOrThrow()
}