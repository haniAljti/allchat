package com.hanialjti.allchat.xmpp

import com.hanialjti.allchat.exception.NotSupportedException
import com.hanialjti.allchat.models.entity.Type
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smackx.chat_markers.element.ChatMarkersElements
import org.jivesoftware.smackx.chatstates.ChatState
import org.jivesoftware.smackx.forward.packet.Forwarded
import org.jxmpp.jid.BareJid
import org.jxmpp.jid.Jid
import org.jxmpp.jid.impl.JidCreate

fun Message.Type.toMessageType() = if (this == Message.Type.groupchat) Type.GroupChat else Type.Chat

fun Jid.isGroupConversation() = this.asDomainBareJid().contains("@conference")

suspend fun String.asJid(): BareJid =
    withContext(Dispatchers.IO) { return@withContext JidCreate.bareFrom(this@asJid) }

fun Message.getStringFrom() = from.localpartOrNull?.toString()

fun ChatState.toConversationState(conversation: String, from: String) = when (this) {
    ChatState.active -> com.hanialjti.allchat.models.ChatState.Active(
        conversation,
        from
    )
    ChatState.composing -> com.hanialjti.allchat.models.ChatState.Composing(
        conversation,
        from
    )
    ChatState.inactive -> com.hanialjti.allchat.models.ChatState.Inactive(
        conversation,
        from
    )
    else -> {
        com.hanialjti.allchat.models.ChatState.Paused(conversation, from)
    }
}

fun XMPPTCPConnection.getOwner() = user?.localpartOrNull?.toString()

fun Message.getChatMarker(): ChatMarker = when {
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
        else -> throw NotSupportedException("This chat marker is not supported")
    }

fun Forwarded<Message>.timestamp() = delayInformation.stamp.time

fun XMPPTCPConnection.getConversationIdFromMessage(message: Message): String {
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