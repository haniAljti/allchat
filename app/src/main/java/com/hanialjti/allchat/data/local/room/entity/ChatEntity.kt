package com.hanialjti.allchat.data.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hanialjti.allchat.R
import com.hanialjti.allchat.data.model.Contact
import com.hanialjti.allchat.data.model.MessageSummary
import com.hanialjti.allchat.presentation.conversation.ContactImage

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey
    val id: String,             // id in the remote service eg. for xmpp its jid
    val owner: String,
    @ColumnInfo(name = "is_group_chat")
    val isGroupChat: Boolean = false,
    val name: String? = null,
    val avatar: String? = null,
    val lastMessage: MessageSummary? = null,
    @ColumnInfo(name = "unread_messages_count")
    val unreadMessages: Int = 0,
)

fun ChatEntity.asContact() = Contact(
    id = id,
    isGroupChat = isGroupChat,
    name = name,
    image = avatar?.let { ContactImage.DynamicImage(avatar) }
        ?: ContactImage.DefaultProfileImage(isGroupChat),
    lastMessage = lastMessage,
    unreadMessages = unreadMessages,
)