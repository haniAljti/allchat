package com.hanialjti.allchat.models.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.hanialjti.allchat.utils.currentTimestamp
import java.util.*

@Entity
data class Conversation(
    @PrimaryKey
    @ColumnInfo(name = "conversationId")
    val id: String = UUID.randomUUID().toString(),
    val lastMessage: String? = null,
    val lastUpdated: Long = currentTimestamp,
    val isGroupChat: Boolean = false,
    val composing: List<String> = listOf(),
    val name: String? = null,
    val imageUrl: String? = null,
    val from: String? = null,                           // Current logged in user
    val to: String? = null,                             // Other User if this is a group chat
    val unreadMessages: Int = 0,
) {
    @Ignore
    val otherComposingUsers =
        if (composing.any { it != "" }) {
            Composing(
                userListString = composing.filter { it != from }.joinToString(),
                count = composing.count { it != from }
            )
        } else null
}

data class Composing(
    val userListString: String,
    val count: Int
)