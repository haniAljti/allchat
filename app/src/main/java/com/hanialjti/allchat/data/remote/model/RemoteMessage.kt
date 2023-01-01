package com.hanialjti.allchat.data.remote.model

import com.hanialjti.allchat.data.local.room.entity.MessageEntity
import com.hanialjti.allchat.data.model.Marker
import com.hanialjti.allchat.data.model.MessageType
import com.hanialjti.allchat.data.model.MessageStatus
import java.time.OffsetDateTime

data class RemoteMessage(
    val id: String,
    val chatId: String? = null,
    val sender: String? = null,
    val messageStatus: MessageStatus,
    val type: MessageType? = null,
    val body: String? = null,
    val thread: String? = null,
    val markers: Map<String, Marker> = mapOf(),
    val timestamp: OffsetDateTime = OffsetDateTime.now(),
    val messageArchiveId: String? = null
)

fun RemoteMessage.asMessageEntity() = MessageEntity(
    externalId = id,
    body = body,
    timestamp = timestamp,
    contactId = chatId,
    senderId = sender,
    status = messageStatus,
    type = type,
    archiveId = messageArchiveId
)