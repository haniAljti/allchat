package com.hanialjti.localdatabase

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hanialjti.allchat.localdatabase.AllChatLocalRoomDatabase
import com.hanialjti.allchat.localdatabase.MessageDao
import com.hanialjti.allchat.models.entity.Media
import com.hanialjti.allchat.models.entity.Message
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
}