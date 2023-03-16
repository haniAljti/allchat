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
abstract class AllChatLocalRoomDatabase : RoomDatabase(), LocalDataStore {
    abstract override fun userDao(): UserDao
    abstract override fun conversationDao(): ConversationDao
    abstract override fun messageDao(): MessageDao
    abstract override fun markerDao(): MessageMarkerDao
    abstract override fun participantDao(): ParticipantDao
    abstract override fun infoDao(): InfoDao
    abstract override fun blockedUserDao(): BlockedUserDao
}

interface LocalDataStore {
    fun userDao(): UserDao
    fun conversationDao(): ConversationDao
    fun messageDao(): MessageDao
    fun markerDao(): MessageMarkerDao
    fun participantDao(): ParticipantDao
    fun infoDao(): InfoDao
    fun blockedUserDao(): BlockedUserDao
}