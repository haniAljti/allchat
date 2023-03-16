package com.hanialjti.allchat.data.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String,
    val owner: String,
    @ColumnInfo(name = "is_group_chat")
    val isGroupChat: Boolean = false,
    @ColumnInfo(name = "unread_messages_count")
    val unreadMessages: Int = 0,
    val participants: Set<String> = setOf()
)