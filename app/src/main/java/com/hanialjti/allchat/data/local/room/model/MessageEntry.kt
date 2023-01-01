package com.hanialjti.allchat.data.local.room.model

import androidx.room.Embedded
import androidx.room.Relation
import com.hanialjti.allchat.data.local.room.entity.*
import com.hanialjti.allchat.data.model.MessageItem
import java.time.ZoneId

data class MessageEntry(
    @Embedded
    val message: MessageEntity,

    @Relation(parentColumn = "sender_id", entityColumn = "external_id")
    val sender: UserEntity? = null,
)

fun MessageEntry.asMessage() = MessageItem.MessageData(
    id = message.externalId,
    body = message.body,
    timestamp = message.timestamp.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime(),
    contactId = message.contactId,
    senderId = sender?.externalId,
    senderImage = sender?.image,
    senderName = sender?.name,
    ownerId = message.ownerId,
    status = message.status,
    read = message.read,
    type = message.type,
    attachment = message.attachment?.asAttachment(),
)

