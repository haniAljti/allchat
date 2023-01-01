package com.hanialjti.allchat.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import com.hanialjti.allchat.data.local.datastore.UserCredentials
import com.hanialjti.allchat.data.local.remote.FakeRemoteMessageSource
import com.hanialjti.allchat.data.local.room.AllChatLocalRoomDatabase
import com.hanialjti.allchat.data.local.room.dao.MessageDao
import com.hanialjti.allchat.data.local.room.entity.ChatEntity
import com.hanialjti.allchat.data.local.room.entity.ParticipantEntity
import com.hanialjti.allchat.data.local.room.entity.UserEntity
import com.hanialjti.allchat.data.model.MessageStatus
import com.hanialjti.allchat.data.remote.model.CallResult
import com.hanialjti.allchat.data.remote.ConnectionManager
import com.hanialjti.allchat.data.repository.ChatRepository
import com.hanialjti.allchat.data.tasks.MessageTasksDataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, AllChatLocalRoomDatabase::class.java).build()
        messageDao = db.messageDao()
        chatRepository = ChatRepository(
            messageTasksDataSource = object: MessageTasksDataSource {
                override fun createAndExecuteSendMessageWork(messageId: Long) {}
            },
            localDb = db,
            messageRemoteDataSource = FakeRemoteMessageSource(),
            connectionManager = object: ConnectionManager {
                override fun observeConnectivityStatus(): Flow<ConnectionManager.Status> {
                    return flow {
                        emit(ConnectionManager.Status.Connected)
                    }
                }

                override fun getUsername(): String = "user0"

                override suspend fun connect(userCredentials: UserCredentials): CallResult<Nothing?> {
                    return CallResult.Success()
                }

                override suspend fun disconnect() {}

                override suspend fun registerWorker(worker: ListenableWorker) {}

                override suspend fun unregisterWorker(worker: ListenableWorker) {}

            }
        )
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
                externalId = "user1@AllChat"
            )
        )
        db.conversationDao().insert(
            ChatEntity(externalId = "user1@AllChat", owner = "user0")
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