package com.hanialjti.allchat.localdatabase

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hanialjti.allchat.models.entity.Conversation
import com.hanialjti.allchat.models.entity.ConversationParticipantsCrossRef
import com.hanialjti.allchat.models.entity.Message
import com.hanialjti.allchat.models.entity.User
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class RoomDatabaseModule {

    @Provides
    @Singleton
    fun provideDb(@ApplicationContext applicationContext: Context): AllChatLocalRoomDatabase {
        return Room.databaseBuilder(
            applicationContext,
            AllChatLocalRoomDatabase::class.java,
            "all-chat-database"
        )
            .addCallback(
                object: RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        db.execSQL("insert into Conversation values(\"con_1\", \"Hey\", 0, 0, \"\", \"First Conversation\", null, \"user_1\", \"user_2\", 15);")
                        db.execSQL("insert into Conversation values(\"con_2\", \"Hello\", 0, 1, \"\", \"Second Conversation\", null, \"user_1\", \"user_3\", 100);")
                        db.execSQL("insert into User values(\"user_2\", \"User 2\", null, 0, 0);")
                        db.execSQL("insert into User values(\"user_3\", \"User 3\", null, 1, 0);")
                    }
                }
            )
            .build()
    }
}

@Database(entities = [User::class, Message::class, Conversation::class, ConversationParticipantsCrossRef::class], version = 1)
@TypeConverters(Converters::class)
abstract class AllChatLocalRoomDatabase: RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
//    abstract fun mediaDao(): MediaDao
}