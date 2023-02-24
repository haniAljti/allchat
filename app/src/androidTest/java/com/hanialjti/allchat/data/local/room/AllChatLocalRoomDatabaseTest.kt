package com.hanialjti.allchat.data.local.room

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hanialjti.allchat.data.local.room.dao.MessageDao
import com.hanialjti.allchat.data.local.room.entity.MarkerEntity
import com.hanialjti.allchat.data.local.room.entity.MessageEntity
import com.hanialjti.allchat.data.local.room.entity.UserEntity
import com.hanialjti.allchat.data.model.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AllChatLocalRoomDatabaseTest {
    private lateinit var messageDao: MessageDao
    private lateinit var db: AllChatLocalRoomDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, AllChatLocalRoomDatabase::class.java).build()
        messageDao = db.messageDao()
    }

    @After
    fun closeDb() = runBlocking {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun verifyMessageSavesAndLoadsCorrectly() = runBlocking {
        val message = initialMessage()
        messageDao.insertOrReplace(message)
        val foundMessage = messageDao.getMessageByRemoteId("fakeId")

        Assert.assertNotNull(foundMessage)
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun verifyMessageIsUpdatedWhenStatusHigher() = runTest {
        val message = initialMessage()
        val user = UserEntity(id = "user1", name = "user1")
        val marker = MarkerEntity(
            userId = "user1",
            messageId = message.externalId ?: "1",
            marker = Marker.Delivered
        )
        db.userDao().insertUser(user)
        db.messageDao().insertOrReplace(message)
        db.markerDao().insert(marker)
        db.markerDao().insert(marker.copy(marker = Marker.Seen))

        assertThat(db.markerDao().getCountForMarker(message.externalId ?: "1", Marker.Seen), Matchers.`is`(1))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun verifyMarkerAddedAndSavedToEmptyListCorrectly() = runTest {
        val message = initialMessage()
        val user = UserEntity(id = "user1", name = "user1")
        val marker = MarkerEntity(
            userId = "user1",
            messageId = message.externalId ?: "1",
            marker = Marker.Seen
        )
        db.userDao().insertUser(user)
        db.messageDao().insertOrReplace(message)
        db.markerDao().insert(marker)

        assertThat(db.markerDao().getCountForMarker(message.externalId ?: "1", Marker.Seen), Matchers.`is`(1))
    }

}

fun initialMessage() = MessageEntity(
    externalId = "fakeId",
    body = "Hallo",
    contactId = "fake@allChat",
    senderId = "remoteUser@AllChat",
    ownerId = "Me@AllChat",
    type = MessageType.Chat
)