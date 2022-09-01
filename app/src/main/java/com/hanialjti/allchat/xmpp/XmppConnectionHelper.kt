package com.hanialjti.allchat.xmpp

import com.hanialjti.allchat.ConnectionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jivesoftware.smack.ConnectionConfiguration
import org.jivesoftware.smack.chat2.ChatManager
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration
import org.jivesoftware.smackx.mam.MamManager
import org.jxmpp.jid.impl.JidCreate

object XmppConnectionHelper: ConnectionManager {

    private val config = XMPPTCPConnectionConfiguration.builder()
        .setHost("192.168.0.42")
        .setPort(5222)
        .setXmppDomain(JidCreate.domainBareFrom("hani@localhost"))
        .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
        .enableDefaultDebugger()
        .build()

    private val connection = XMPPTCPConnection(config)
    private val chatManager = ChatManager.getInstanceFor(connection)
    private val mamManager = MamManager.getInstanceFor(connection)

    override suspend fun connect(username: String, password: String) = withContext(Dispatchers.IO) {
        try {
            connection.connect().login(username, password)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun queryMostRecentPage(conversation: String, pageSize: Int): List<com.hanialjti.allchat.models.entity.Message> {
        return try {
            val messagePage = mamManager.queryMostRecentPage(JidCreate.bareFrom(conversation), pageSize)
            val stanzas = messagePage.page.messages
            val forwardedMessages = messagePage.page.forwarded
            stanzas.zip(forwardedMessages) { stanza, forwarded ->
                com.hanialjti.allchat.models.entity.Message(
                    id = stanza.stanzaId,
                    body = stanza.body,
                    timestamp = forwarded.delayInformation.stamp.time,
                    conversation = stanza.from.asEntityBareJidIfPossible()?.toString(),
                    from = stanza.from.asEntityBareJidIfPossible()?.toString(),
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            listOf()
        }
    }

//    fun listenForOneOnOneIncomingMessages() = callbackFlow<com.hanialjti.allchat.models.entity.Message> {
//        try {
//            chatManager.addIncomingListener { from, message, chat ->
//                com.hanialjti.allchat.models.entity.Message(
//                    id = message.stanzaId,
//                    body = message.body,
//                    timestamp = currentTimestamp,
//                    conversation = from.toString(),
//                    from = from.toString(),
//                    status = "received",
//                    type = message.type.name,
//                    media = message.
//                )
//            }
//        }
//    }

    suspend fun sendMessage(message: String) = withContext(Dispatchers.IO) {
        try {
            val message: Message = connection.stanzaFactory
                .buildMessageStanza()
                .to("admin@localhost")
                .setBody(message)
                .build()

            connection.sendStanza(message)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            connection.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}