package com.hanialjti.allchat.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.hanialjti.allchat.data.local.room.dao.*
import com.hanialjti.allchat.data.local.room.entity.*

@Database(
    entities = [
        UserEntity::class,
        MessageEntity::class,
        ChatEntity::class,
        ParticipantEntity::class,
        MarkerEntity::class,
        InfoEntity::class,
        BlockedUserEntity::class],
    version = 1
)
@TypeConverters(Converters::class)
abstract class AllChatLocalRoomDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun markerDao(): MessageMarkerDao
    abstract fun participantDao(): ParticipantDao
    abstract fun infoDao(): InfoDao
    abstract fun blockedUserDao(): BlockedUserDao
}