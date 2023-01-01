package com.hanialjti.allchat.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hanialjti.allchat.data.local.room.AllChatLocalRoomDatabase
import com.hanialjti.allchat.data.local.room.dao.ConversationDao
import com.hanialjti.allchat.data.local.room.dao.UserDao
import com.hanialjti.allchat.data.repository.ConversationRepository
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConversationRepositoryTest {

    private lateinit var db: AllChatLocalRoomDatabase
    private lateinit var conversationDao: ConversationDao
    private lateinit var userDao: UserDao
    private lateinit var conversationRepository: ConversationRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context,
            AllChatLocalRoomDatabase::class.java
        ).build()
        conversationDao = db.conversationDao()
        userDao = db.userDao()
    }

    @After
    fun closeDb() {
        db.close()
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