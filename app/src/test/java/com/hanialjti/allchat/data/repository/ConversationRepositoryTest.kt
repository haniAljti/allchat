package com.hanialjti.allchat.data.repository

import app.cash.turbine.test
import com.hanialjti.allchat.MainDispatcherRule
import com.hanialjti.allchat.data.local.room.AllChatLocalRoomDatabase
import com.hanialjti.allchat.data.local.room.dao.ConversationDao
import com.hanialjti.allchat.data.local.room.dao.InfoDao
import com.hanialjti.allchat.data.local.room.dao.ParticipantDao
import com.hanialjti.allchat.data.local.room.entity.ChatEntity
import com.hanialjti.allchat.data.remote.ChatRemoteDataSource
import com.hanialjti.allchat.data.remote.model.RoomState
import com.hanialjti.allchat.data.remote.model.MultiUserChatStateUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
    private val localDataSourceMock = mock<AllChatLocalRoomDatabase>()
    private val conversationDao = mock<ConversationDao>()
    private val participantDao = mock<ParticipantDao>()
    private val infoDao = mock<InfoDao>()
    private val userRepositoryMock = mock<UserRepository>()
    private val authRepositoryMock = mock<AuthRepository>()
    private val fileRepositoryMock = mock<FileRepository>()

    @get:Rule
    private val mainDispatcherRule = MainDispatcherRule()

    private lateinit var conversationRepository: ConversationRepository

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        Dispatchers.setMain(mainDispatcherRule.testDispatcher)
        conversationRepository = ConversationRepository(
            chatLocalDataSource = localDataSourceMock,
            remoteDataStore = remoteChatMock,
            userRepository = userRepositoryMock,
            authenticationRepository = authRepositoryMock,
            fileRepository = fileRepositoryMock,
            dispatcher = mainDispatcherRule.testDispatcher,
            externalScope = TestScope(),
            conversationDao = conversationDao,
            participantDao = participantDao,
            infoDao = infoDao
        )
//        `when`(localDataSourceMock.conversationDao()).thenReturn(conversationDao)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `verify chatUpdateStream emits all chats`() = runTest {
        val chatFlow = flowOf(
            MultiUserChatStateUpdate(
                RoomState("1@test")
            ),
            MultiUserChatStateUpdate(
                RoomState("2@test")
            )
        )
        val authenticatedFlow = flow {
            emit("user")
        }

        doReturn(chatFlow).`when`(remoteChatMock).chatUpdatesStream()
        doReturn(authenticatedFlow).`when`(authRepositoryMock).loggedInUserStream

        `when`(conversationDao.getConversationByRemoteId(any()))
            .thenReturn(
                ChatEntity(id = "1", owner = "user")
            )
//        doReturn(ChatEntity(id = "1", owner = "user")).`when`(conversationDao)
//            .getConversationByRemoteId(any())


        conversationRepository
            .listenForConversationUpdates()
            .test {
                assertEquals("1@test", awaitItem().chatId)
                assertEquals("2@test", awaitItem().chatId)
                awaitComplete()
            }
    }
}