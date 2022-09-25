package com.hanialjti.allchat.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hanialjti.allchat.data.local.room.AllChatLocalRoomDatabase
import com.hanialjti.allchat.data.local.room.ConversationDao
import com.hanialjti.allchat.data.local.room.UserDao
import com.hanialjti.allchat.data.local.room.entity.ConversationAndUser
import com.hanialjti.allchat.data.local.room.entity.Conversation
import com.hanialjti.allchat.data.local.room.entity.User
import com.hanialjti.allchat.data.remote.xmpp.XmppConnectionHelper
import com.hanialjti.allchat.data.repository.ConversationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

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

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun insertConversationAndUser() = runTest {
        val id = "admin@localhost"
        val name = "Admin"
        val myId = "user_1"
        val conversationAndUser = ConversationAndUser(
            conversation = Conversation(
                id,
                isGroupChat = false,
                from = "user_1",
                to = "admin@localhost"
            ),
            user = User(
                id = id,
                name = name
            )
        )
        val xmppConnectionHelper = mock<XmppConnectionHelper>()
        conversationRepository = ConversationRepository(db, xmppConnectionHelper)
        conversationRepository.insertConversationAndUser(conversationAndUser)
        val conversation =
            conversationDao.getConversationAndUserById(conversationAndUser.conversation.id)

        assertEquals(conversation?.user, conversationAndUser.user)
        assertEquals(conversation?.conversation, conversationAndUser.conversation)

    }
}