package com.hanialjti.allchat.data.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hanialjti.allchat.data.model.Contact
import org.jetbrains.annotations.NotNull

@Entity(tableName = "chats", primaryKeys = ["external_id", "owner"])
data class ChatEntity(
    @ColumnInfo(name = "external_id")
    val externalId: String,             // id in the remote service eg. for xmpp its jid
    val owner: String,

    @ColumnInfo(name = "is_group_chat")
    val isGroupChat: Boolean = false,
    val name: String? = null,
    val image: String? = null,
    @ColumnInfo(name = "unread_messages_count")
    val unreadMessages: Int = 0,
)

data class ConversationInfoEntity(
    @ColumnInfo(name = "external_id")
    val externalId: String,
    val name: String?,
    val image: String?,
    val owner: String,
)
