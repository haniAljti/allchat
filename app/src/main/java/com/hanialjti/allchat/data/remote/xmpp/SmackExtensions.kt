package com.hanialjti.allchat.data.remote.xmpp

import com.hanialjti.allchat.data.local.room.entity.Type
import com.hanialjti.allchat.data.remote.xmpp.model.ChatMarker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.packet.Stanza
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smackx.chat_markers.element.ChatMarkersElements
import org.jivesoftware.smackx.chatstates.ChatState
import org.jivesoftware.smackx.forward.packet.Forwarded
import org.jxmpp.jid.BareJid
import org.jxmpp.jid.Jid
import org.jxmpp.jid.impl.JidCreate

fun Message.Type.toMessageType() = if (this == Message.Type.groupchat) Type.GroupChat else Type.Chat

fun Jid.isGroupConversation() = this.asDomainBareJid().contains("@conference.")

suspend fun String.asJid(): BareJid =
    withContext(Dispatchers.IO) { return@withContext JidCreate.bareFrom(this@asJid) }

fun Stanza.getStringFrom() = from.localpartOrNull?.toString()

fun Type.toXmppMessageType() = when (this) {
    Type.Chat -> Message.Type.chat
    Type.GroupChat -> Message.Type.groupchat
}

fun com.hanialjti.allchat.data.local.room.entity.Message.toMessageStanza(connection: XMPPTCPConnection): Message =
    connection.stanzaFactory
        .buildMessageStanza()
        .to(conversation)
        .ofType(Message.Type.chat)
        .setBody(body)
        .addExtension(ChatMarkersElements.MarkableExtension.INSTANCE)
        .build()

fun ChatState.toConversationState(conversation: String, from: String) = when (this) {
    ChatState.active -> com.hanialjti.allchat.presentation.conversation.ChatState.Active(
        conversation,
        from
    )
    ChatState.composing -> com.hanialjti.allchat.presentation.conversation.ChatState.Composing(
        conversation,
        from
    )
    ChatState.inactive -> com.hanialjti.allchat.presentation.conversation.ChatState.Inactive(
        conversation,
        from
    )
    else -> {
        com.hanialjti.allchat.presentation.conversation.ChatState.Paused(conversation, from)
    }
}

fun XMPPTCPConnection.getOwner() = user?.localpartOrNull?.toString()

fun Message.getChatMarker(): ChatMarker? = when {
    hasExtension(ChatMarkersElements.DisplayedExtension.QNAME) -> {
        val displayedMarker = getExtension(ChatMarkersElements.DisplayedExtension.QNAME)
                as ChatMarkersElements.DisplayedExtension
        ChatMarker.Displayed(displayedMarker.id)
    }
    hasExtension(ChatMarkersElements.AcknowledgedExtension.QNAME) -> {
        val acknowledgedMarker = getExtension(ChatMarkersElements.AcknowledgedExtension.QNAME)
                as ChatMarkersElements.AcknowledgedExtension
        ChatMarker.Acknowledged(acknowledgedMarker.id)
    }
    hasExtension(ChatMarkersElements.ReceivedExtension.QNAME) -> {
        val receivedMarker = getExtension(ChatMarkersElements.ReceivedExtension.QNAME)
                as ChatMarkersElements.ReceivedExtension
        ChatMarker.Received(receivedMarker.id)
    }
    else -> null
}

fun Stanza.isMessageAck(): Boolean =
    this is Message && type == Message.Type.chat && to != null && stanzaId != null

fun Forwarded<Message>.timestamp() = delayInformation.stamp.time

fun XMPPTCPConnection.getConversationIdFromStanza(message: Message): String {
    return if (message.type == Message.Type.groupchat)
        message.to.asBareJid().toString()
    else {
        if (message.from.localpartOrNull?.toString() == getOwner()) {
            message.to.asBareJid().toString()
        } else {
            message.from.asBareJid().toString()
        }
    }
}