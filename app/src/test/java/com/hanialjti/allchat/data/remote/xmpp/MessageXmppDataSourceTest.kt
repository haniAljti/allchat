package com.hanialjti.allchat.data.remote.xmpp

import com.hanialjti.allchat.MainDispatcherRule
import com.hanialjti.allchat.data.local.room.entity.MessageEntity
import com.hanialjti.allchat.data.model.MessageType
import com.hanialjti.allchat.data.model.MessageStatus
import com.hanialjti.allchat.data.remote.model.CallResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.jivesoftware.smack.ConnectionConfiguration
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import kotlin.test.assertEquals
import kotlin.test.assertIsNot

class MessageXmppDataSourceTest {

    private val config = XMPPTCPConnectionConfiguration.builder()
        .setHost("192.168.0.42")
        .setPort(5222)
        .setXmppDomain("localhost")
        .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
        .build()

    private lateinit var messageXmppDataSource: MessageXmppDataSource
    private var firstConnection: XMPPTCPConnection = XMPPTCPConnection(config)
    private var secondConnection: XMPPTCPConnection = XMPPTCPConnection(config)
    private val mucManager = mock<MucManager>()

    @get:Rule
    private val mainDispatcherRule = MainDispatcherRule()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setupConnection() {
        messageXmppDataSource = MessageXmppDataSource(
            firstConnection,
            mucManager,
            mainDispatcherRule.testDispatcher
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun verifyAckMessageFor1To1CorrectlyMapped() = runTest(mainDispatcherRule.testDispatcher) {
        firstConnection.connect().login("hani", "15960400")

        val message = messageXmppDataSource.sendMessage(
            MessageEntity(
                body = "hallo",
                contactId = "hani2@localhost",
                type = MessageType.Chat
            ),
            null,
            true
        )

        assertIsNot<CallResult.Error>(message)

        val networkMessage = messageXmppDataSource.observeAcknowledgmentMessages().first()

        assertEquals((message as CallResult.Success).data, networkMessage.id)
        assertEquals(MessageStatus.Sent, networkMessage.messageStatus)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun verifyAckMessageForGroupChatCorrectlyMapped() = runTest(mainDispatcherRule.testDispatcher) {
        firstConnection.connect().login("hani", "15960400")

        val message = messageXmppDataSource.sendMessage(
            MessageEntity(
                body = "hallo",
                contactId = "ebb76127-17f9-437f-bde5-cceb2efc3215@conference.localhost",
                type = MessageType.GroupChat
            ),
            null,
            true
        )

        assertIsNot<CallResult.Error>(message, "Message not sent")

        val networkMessage = messageXmppDataSource.observeAcknowledgmentMessages().first()

        assertEquals((message as CallResult.Success).data, networkMessage.id)
        assertEquals(MessageStatus.Sent, networkMessage.messageStatus)
    }
}