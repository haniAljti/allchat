package com.hanialjti.allchat.localdatabase

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.hanialjti.allchat.models.entity.*

//@InstallIn(SingletonComponent::class)
//@Module
//class RoomDatabaseModule {
//
//    @Provides
//    @Singleton
//    fun provideDb(@ApplicationContext applicationContext: Context): AllChatLocalRoomDatabase {
//        return Room.databaseBuilder(
//            applicationContext,
//            AllChatLocalRoomDatabase::class.java,
//            "all-chat-database"
//        ).build()
//    }
//}

@Database(entities = [User::class, Message::class, Conversation::class, ConversationParticipantsCrossRef::class], version = 1)
@TypeConverters(Converters::class, MessageStatusConverter::class)
abstract class AllChatLocalRoomDatabase: RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
//    abstract fun mediaDao(): MediaDao
}