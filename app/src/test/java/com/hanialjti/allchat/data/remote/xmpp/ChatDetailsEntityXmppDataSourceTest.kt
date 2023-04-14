package com.hanialjti.allchat.data.remote.xmpp

import app.cash.turbine.test
import com.hanialjti.allchat.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.jivesoftware.smack.roster.Roster
import org.jivesoftware.smack.roster.RosterListener
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smackx.bookmarks.BookmarkManager
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import kotlin.test.assertEquals

internal class ChatDetailsEntityXmppDataSourceTest {

    private val connection = mock<XMPPTCPConnection>()
    private val roster = mock<Roster>()
    private val rosterManager = mock<RosterManager>()
    private val mucManager = mock<MucManager>()
    private val bookmarkManager = mock<BookmarkManager>()

    private lateinit var chatDataSource: ChatXmppDataSource

    @get:Rule
    private val mainDispatcherRule = MainDispatcherRule()

    @Before
    fun setupConnection() {

        chatDataSource = ChatXmppDataSource(
            connection,
            rosterManager,
            mucManager
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `verify that flow collects 2 chat objects when emitted`() = runTest {

        val jid1 = "1".asJid()
        val jid2 = "2".asJid()

        doAnswer {
            val argument = it.getArgument<RosterListener>(0)
            argument.entriesAdded(listOf(jid1, jid2))
            return@doAnswer null
        }.`when`(roster).addRosterListener(any())

        chatDataSource.chatUpdatesStream().test {
            assertEquals(jid1.asBareJid().toString(), awaitItem().chatId)
            assertEquals(jid2.asBareJid().toString(), awaitItem().chatId)
        }
    }
}