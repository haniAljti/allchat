package com.hanialjti.localdatabase

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hanialjti.allchat.data.local.room.AllChatLocalRoomDatabase
import com.hanialjti.allchat.data.local.room.MessageDao
import com.hanialjti.allchat.data.local.room.entity.Media
import com.hanialjti.allchat.data.local.room.entity.Message
import com.hanialjti.allchat.data.local.room.entity.Status
import com.hanialjti.allchat.common.utils.currentTimestamp
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.equalTo
import org.junit.After
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
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun writeMessageMediaAndReadInList() {
        val message = Message(
            media = Media(
                url = "url"
            ),
            conversation = "1",
        )
//        messageDao.insert(message)
        val messages = messageDao.getAllByConversation("1")
    }

    @Test
    fun messageStatus_lessThanSaved_shouldNotSave() = runBlocking {
        val message = Message(
            id = 1,
            timestamp = currentTimestamp,
            status = Status.Seen,
            remoteId = "1"
        )

        db.messageDao().insertOrIgnore(message)
        db.messageDao().updateMessageStatus(
            "1", Status.Acknowledged
        )

        val savedMessage = db.messageDao().getMessageById(1)
        assertThat(savedMessage.status, equalTo(Status.Seen))
    }

    @Test
    fun messageStatus_greaterThanSaved_shouldSave() = runBlocking {
        val message = Message(
            id = 1,
            timestamp = currentTimestamp,
            status = Status.Pending,
            remoteId = "1"
        )

        db.messageDao().insertOrIgnore(message)
        db.messageDao().updateMessageStatus(
            "1", Status.Acknowledged
        )

        val savedMessage = db.messageDao().getMessageById(1)
        assertThat(savedMessage.status, equalTo(Status.Acknowledged))
    }
}