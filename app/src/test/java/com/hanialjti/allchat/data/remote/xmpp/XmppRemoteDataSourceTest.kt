package com.hanialjti.allchat.data.remote.xmpp

import com.hanialjti.allchat.MainDispatcherRule
import com.hanialjti.allchat.data.local.room.entity.MessageEntity
import com.hanialjti.allchat.data.model.MessageType
import com.hanialjti.allchat.data.model.MessageStatus
import com.hanialjti.allchat.data.remote.model.CallResult
import com.hanialjti.allchat.data.remote.xmpp.model.PingConfigurations
import com.hanialjti.allchat.data.remote.xmpp.model.XmppConnectionConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.jivesoftware.smack.ConnectionConfiguration
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIsNot

class XmppRemoteDataSourceTest {

    private val config = XMPPTCPConnectionConfiguration.builder()
        .setHost("192.168.0.42")
        .setPort(5222)
        .setXmppDomain("localhost")
        .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
        .build()

    private lateinit var xmppRemoteDataSource: XmppRemoteDataSource
    private var firstConnection: XMPPTCPConnection = XMPPTCPConnection(config)
    private var secondConnection: XMPPTCPConnection = XMPPTCPConnection(config)

    @get:Rule
    private val mainDispatcherRule = MainDispatcherRule()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setupConnection() {
        xmppRemoteDataSource = XmppRemoteDataSource(
            firstConnection,
            XmppConnectionConfig(
                host = "192.168.0.42",
                domain = "localhost",
                port = 5222,
                pingConfigurations = PingConfigurations.DisablePingMessages,
                chatMarkersEnabled = true,
                enableChatStateNotifications = true
            ),
            mainDispatcherRule.testDispatcher
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun verifyAckMessageFor1To1CorrectlyMapped() = runTest(mainDispatcherRule.testDispatcher) {
        firstConnection.connect().login("hani", "15960400")

        val message = xmppRemoteDataSource.sendMessage(
            MessageEntity(
                body = "hallo",
                contactId = "hani2@localhost",
                type = MessageType.Chat
            ),
            true
        )

        assertIsNot<CallResult.Error>(message)

        val networkMessage = xmppRemoteDataSource.observeAcknowledgmentMessages().first()

        assertEquals((message as CallResult.Success).data, networkMessage.id)
        assertEquals(MessageStatus.Sent, networkMessage.messageStatus)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun verifyAckMessageForGroupChatCorrectlyMapped() = runTest(mainDispatcherRule.testDispatcher) {
        firstConnection.connect().login("hani", "15960400")

        val message = xmppRemoteDataSource.sendMessage(
            MessageEntity(
                body = "hallo",
                contactId = "ebb76127-17f9-437f-bde5-cceb2efc3215@conference.localhost",
                type = MessageType.GroupChat
            ),
            true
        )

        assertIsNot<CallResult.Error>(message, "Message not sent")

        val networkMessage = xmppRemoteDataSource.observeAcknowledgmentMessages().first()

        assertEquals((message as CallResult.Success).data, networkMessage.id)
        assertEquals(MessageStatus.Sent, networkMessage.messageStatus)
    }
}