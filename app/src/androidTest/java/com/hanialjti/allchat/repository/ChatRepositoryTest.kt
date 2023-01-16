package com.hanialjti.allchat.repository

import com.hanialjti.allchat.data.local.room.AllChatLocalRoomDatabase
import com.hanialjti.allchat.data.local.room.dao.MessageDao
import com.hanialjti.allchat.data.local.room.entity.ChatEntity
import com.hanialjti.allchat.data.local.room.entity.ParticipantEntity
import com.hanialjti.allchat.data.local.room.entity.UserEntity
import com.hanialjti.allchat.data.model.MessageStatus
import com.hanialjti.allchat.data.repository.ChatRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class ChatRepositoryTest {

    private lateinit var messageDao: MessageDao
    private lateinit var db: AllChatLocalRoomDatabase
    private lateinit var chatRepository: ChatRepository

    @Before
    fun createDb() {
//        val context = ApplicationProvider.getApplicationContext<Context>()
//        db = Room.inMemoryDatabaseBuilder(
//            context, AllChatLocalRoomDatabase::class.java).build()
//        messageDao = db.messageDao()
//        chatRepository = ChatRepository(
//            messageTasksDataSource = object: MessageTasksDataSource {
//                override fun createAndExecuteSendMessageWork(messageId: Long) {}
//            },
//            localDb = db,
//            messageRemoteDataSource = FakeRemoteMessageSource(),
//            connectionManager = object: ConnectionManager {
//                override fun observeConnectivityStatus(): Flow<ConnectionManager.Status> {
//                    return flow {
//                        emit(ConnectionManager.Status.Connected)
//                    }
//                }
//
//                override fun getUsername(): String = "user0"
//                override fun getConfig(): ConnectionConfig {
//                    TODO("Not yet implemented")
//                }
//
//                override suspend fun connect(userCredentials: UserCredentials) {
//                }
//
//                override suspend fun disconnect() {}
//
//                override suspend fun registerWorker(worker: ListenableWorker) {}
//
//                override suspend fun unregisterWorker(worker: ListenableWorker) {}
//                override suspend fun updateMyPresence(presence: Presence) {
//                    TODO("Not yet implemented")
//                }
//
//                override suspend fun onResume() {
//                    TODO("Not yet implemented")
//                }
//
//                override suspend fun onPause() {
//                    TODO("Not yet implemented")
//                }
//
//            },
//
//        )
    }

    @After
    fun closeDb() = runBlocking {
        db.close()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun verifyMarkerSavedCorrectly() = runTest {
        db.userDao().insertUser(
            UserEntity(
                id = "user1@AllChat"
            )
        )
        db.conversationDao().insert(
            ChatEntity(id = "user1@AllChat", owner = "user0")
        )
        db.participantDao().insertParticipants(
            ParticipantEntity(
                chatId = "user1@AllChat",
                owner = "user0",
                userId = "user1@AllChat",
            )
        )

        chatRepository.syncMessages("user0")

        val message = db.messageDao().getMessageByRemoteId("message1")
        Assert.assertNotNull(message)
        Assert.assertEquals(MessageStatus.Seen, message?.status)
    }


}