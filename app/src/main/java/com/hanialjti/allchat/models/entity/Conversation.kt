package com.hanialjti.allchat.models.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.hanialjti.allchat.utils.currentTimestamp
import java.util.*

@Entity(primaryKeys = ["conversationId", "from"])
data class Conversation(
    @ColumnInfo(name = "conversationId")
    val id: String = UUID.randomUUID().toString(),
    val lastMessage: String? = null,
    val lastUpdated: Long? = null,
    val isGroupChat: Boolean = false,
    val states: MutableMap<String?, State?> = mutableMapOf(),
    val name: String? = null,
    val imageUrl: String? = null,
    val from: String,                           // Current logged in user
    val to: String? = null,                             // Other User if this is a group chat
    val unreadMessages: Int = 0,
) {
    @Ignore
    val otherComposingUsers = states
        .filter { !it.key.isNullOrEmpty() && it.value == State.Composing }
        .keys
        .filterNotNull()
}

enum class State { Active, Inactive, Composing, Paused }

data class ConversationInfo(
    @ColumnInfo(name = "conversationId")
    val id: String,
    val name: String?,
    val imageUrl: String?,
    val from: String,
)

data class Composing(
    val userListString: String,
    val count: Int
)