package com.hanialjti.allchat.data.repository

import app.cash.turbine.test
import com.hanialjti.allchat.MainDispatcherRule
import com.hanialjti.allchat.data.local.room.AllChatLocalRoomDatabase
import com.hanialjti.allchat.data.local.room.dao.ConversationDao
import com.hanialjti.allchat.data.local.room.entity.ChatEntity
import com.hanialjti.allchat.data.remote.ChatRemoteDataSource
import com.hanialjti.allchat.data.remote.model.NewContactUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import kotlin.test.assertEquals

class ConversationRepositoryTest {

    private val remoteChatMock = mock<ChatRemoteDataSource>()
//    private val localDataSourceMock = mock<AllChatLocalRoomDatabase>()
    private val userRepositoryMock = mock<UserRepository>()
    private val authRepositoryMock = mock<AuthenticationRepository>()
    private val infoRepositoryMock = mock<InfoRepository>()
    private val conversationDao = mock<ConversationDao>()

    @get:Rule
    private val mainDispatcherRule = MainDispatcherRule()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val conversationRepository = ConversationRepository(
        remoteDataStore = remoteChatMock,
        conversationDao = conversationDao,
        userRepository = userRepositoryMock,
        authenticationRepository = authRepositoryMock,
        infoRepository = infoRepositoryMock,
        dispatcher = mainDispatcherRule.testDispatcher,
        externalScope = TestScope()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        Dispatchers.setMain(mainDispatcherRule.testDispatcher)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `verify chatUpdateStream emits all chats`() = runTest {
        val chatFlow = flowOf(
            NewContactUpdate("1@test", false),
            NewContactUpdate("2@test", false)
        )
        val authenticatedFlow = flow {
            delay(3000)
            emit("user")
        }

        doReturn(chatFlow).`when`(remoteChatMock).chatUpdatesStream()
        doReturn(authenticatedFlow).`when`(authRepositoryMock).loggedInUserStream
        `when`(conversationDao.getConversationByRemoteId(any()))
            .thenReturn(
                ChatEntity(id = "1", owner = "user")
            )

        conversationRepository
            .listenForConversationUpdates()
            .test {
                assertEquals("1@test", awaitItem())
                assertEquals("2@test", awaitItem())
                awaitComplete()
            }
    }
}