package com.hanialjti.allchat.repository

import MainDispatcherRule
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hanialjti.allchat.data.local.room.AllChatLocalRoomDatabase
import com.hanialjti.allchat.data.local.room.dao.ConversationDao
import com.hanialjti.allchat.data.local.room.dao.InfoDao
import com.hanialjti.allchat.data.model.Role
import com.hanialjti.allchat.data.remote.ChatRemoteDataSource
import com.hanialjti.allchat.data.remote.model.RemoteParticipant
import com.hanialjti.allchat.data.remote.model.RoomState
import com.hanialjti.allchat.data.remote.model.MultiUserChatStateUpdate
import com.hanialjti.allchat.data.repository.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.time.OffsetDateTime

@RunWith(AndroidJUnit4::class)
class ConversationRepositoryTest {

    private lateinit var conversationRepository: ConversationRepository

    private lateinit var db: AllChatLocalRoomDatabase
    private lateinit var conversationDao: ConversationDao
    private lateinit var infoDao: InfoDao
    private val userRepository = mock<UserRepository>()
    private val chatRemoteDataSource = mock<ChatRemoteDataSource>()
    private val authenticationRepository = mock<AuthRepository>()
    private val fileRepository = mock<FileRepository>()

    @get:Rule
    private val mainDispatcherRule = MainDispatcherRule()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context,
            AllChatLocalRoomDatabase::class.java
        ).build()
        conversationDao = db.conversationDao()
        infoDao = db.infoDao()
        conversationRepository = ConversationRepository(
            db,
            chatRemoteDataSource,
            userRepository,
            authenticationRepository,
            fileRepository,
            dispatcher = mainDispatcherRule.testDispatcher,
            externalScope = TestScope(),
            conversationDao = conversationDao,
            infoDao = infoDao
        )
    }

    @After
    fun closeDb() {
        db.close()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun verifyWhenRoomStateChangesThenSavesCorrectly() = runTest {
        val chatFlow = flow {
            emit(
                MultiUserChatStateUpdate(
                    RoomState("1@test")
                )
            )

            kotlinx.coroutines.delay(200)

            emit(
                MultiUserChatStateUpdate(
                    RoomState(
                        id = "1@test",
                        description = "Desc",
                        createdAt = OffsetDateTime.now(),
                        createdBy = "user",
                        avatarUrl = null,
                        subject = "subject",
                        participants = mapOf("user" to RemoteParticipant(id = "user", role = Role.Owner))
                    )
                )
            )
        }


        val authenticatedFlow = flow {
            emit("user")
        }

        doReturn(chatFlow).`when`(chatRemoteDataSource).chatUpdatesStream()
        doReturn(authenticatedFlow).`when`(authenticationRepository).loggedInUserStream

        conversationRepository.listenForConversationUpdates().collect()

        val chat = conversationDao.getConversationByRemoteId("1@test")
        val info = infoDao.getOne("1@test")

        assertNotNull(chat)
        assertNotNull(info)
        assertEquals("1@test", chat?.id)
        assertEquals("subject", info?.nickname)
    }

//    @OptIn(ExperimentalCoroutinesApi::class)
//    @Test
//    fun insertConversationAndUser() = runTest {
//        val id = "admin@localhost"
//        val name = "Admin"
//        val myId = "user_1"
//        val conversationEntityAndUser = ConversationAndUser(
//            conversationEntity = ChatEntity(
//                1,
//                externalId = id,
//                isGroupChat = false,
//                owner = "user_1",
//                to = "admin@localhost"
//            ),
//            user = UserEntity(
//                id = 1,
//                externalId = id,
//                name = name
//            )
//        )
//        val xmppRemoteDataSource = mock<XmppRemoteDataSource>()
//        conversationRepository = ConversationRepository(db, xmppRemoteDataSource)
//        conversationRepository.insertConversationAndUser(conversationEntityAndUser)
//        val conversation =
//            conversationDao.getConversationAndUserById(conversationEntityAndUser.conversationEntity.id)
//
//        assertEquals(conversation?.user, conversationEntityAndUser.user)
//        assertEquals(conversation?.conversationEntity, conversationEntityAndUser.conversationEntity)
//
//    }
}