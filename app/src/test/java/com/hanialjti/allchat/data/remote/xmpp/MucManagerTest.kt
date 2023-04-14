package com.hanialjti.allchat.data.remote.xmpp

import com.hanialjti.allchat.MainDispatcherRule
import com.hanialjti.allchat.data.remote.model.CallResult
import com.hanialjti.allchat.data.remote.model.RoomState
import com.hanialjti.allchat.data.remote.xmpp.model.RoomInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.jivesoftware.smack.packet.MessageBuilder
import org.jivesoftware.smack.packet.Stanza
import org.jivesoftware.smack.packet.StanzaBuilder
import org.jivesoftware.smack.packet.StanzaFactory
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smackx.bookmarks.BookmarkManager
import org.jivesoftware.smackx.muc.MucConfigFormManager
import org.jivesoftware.smackx.muc.MultiUserChat
import org.jivesoftware.smackx.muc.MultiUserChatManager
import org.jivesoftware.smackx.pep.PepManager
import org.jivesoftware.smackx.xdata.form.FillableForm
import org.jivesoftware.smackx.xdata.form.Form
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.*
import kotlin.test.assertIs


internal class MucManagerTest {

    @get:Rule
    private val mainDispatcherRule = MainDispatcherRule()

    private val connection = mock<XMPPTCPConnection>()
    private val clientDataSource = mock<XmppClientDataSource>()
    private val multiUserChatManager = mock<MultiUserChatManager>()
    private val mucRoom = mock<MultiUserChat>()
    private val bookmarkManager = mock<BookmarkManager>()
    private val pepManager = mock<PepManager>()
    private val mucConfigFormManager= mock<MucConfigFormManager>()
    private val fillableForm = mock<FillableForm>()
    private val form = mock<Form>()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val testScope = TestScope(mainDispatcherRule.testDispatcher)

    @OptIn(ExperimentalCoroutinesApi::class)
    private var mucManager = MucManager(
        connection,
        clientDataSource,
        testScope,
        mainDispatcherRule.testDispatcher,
        multiUserChatManager,
        bookmarkManager,
        pepManager
    )

    @Before
    fun setup() {
        val roomStates = flowOf(
            RoomState(
                "chat1"
            ),
            RoomState(
                "chat2"
            )
        )
//        doReturn(roomStates).`when`(clientDataSource.chatRooms)

    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun updateChatInfo() = testScope.runTest {


        val updateResult = mucManager.updateChatInfo(
            chatId = "chat1",
            description = "desc",
            avatarUrl = null,
            subject = "subject"
        )

        assertIs<CallResult.Success<Boolean>>(updateResult)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `updateChatInfo should update the chat room info correctly`() = runTest {
        // Given
        val stanzaFactory = mock<StanzaFactory>()
        val chatId = "chat_id"
        val description = "description"
        val avatarUrl = "avatar_url"
        val subject = "subject"
        val oldState = flowOf(
            setOf(
                RoomState(
                    id = chatId,
                    description = description,
                    avatarUrl = avatarUrl,
                    subject = subject
                )
            )
        )
        `when`(clientDataSource.chatRooms).thenReturn(oldState)
        `when`(multiUserChatManager.getMultiUserChat(anyOrNull())).thenReturn(mucRoom)
        `when`(connection.stanzaFactory).thenReturn(stanzaFactory)
        `when`(stanzaFactory.buildMessageStanza()).thenReturn(StanzaBuilder.buildMessage("stanza_id"))
        `when`(mucRoom.configurationForm).thenReturn(form)
        `when`(form.fillableForm).thenReturn(fillableForm)
        `when`(fillableForm.hasField(any())).thenReturn(true)

        // When
        val result =
            mucManager.updateChatInfo(chatId, "new_description", "new_avatar_url", "new_subject")

        // Then
        assertIs<CallResult.Success<Boolean>>(result)
        verify(clientDataSource, never()).addChatRooms(any())
    }
}