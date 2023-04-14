package com.hanialjti.allchat.data.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hanialjti.allchat.data.model.Role
import java.time.OffsetDateTime

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String,
    val owner: String,
    @ColumnInfo(name = "is_group_chat")
    val isGroupChat: Boolean = false,
    val description: String? = null,
    @ColumnInfo(name = "unread_messages_count")
    val unreadMessages: Int = 0,
    @ColumnInfo(name = "created_at")
    val createdAt: OffsetDateTime? = null
) {
    data class Participant(
        val userId: String,
        val state: ChatState = ChatState.Inactive,
        val role: Role = Role.Participant
    )

    enum class ChatState(val value: Int) { Inactive(0), Active(1), Composing(2), Paused(3) }
}