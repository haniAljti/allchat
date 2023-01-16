package com.hanialjti.allchat.data.local.room.model

import androidx.room.Embedded
import androidx.room.Relation
import com.hanialjti.allchat.data.local.room.entity.*
import com.hanialjti.allchat.data.model.MessageItem
import com.hanialjti.allchat.presentation.chat.Attachment
import java.time.ZoneId

data class MessageEntry(
    @Embedded
    val message: MessageEntity,

    @Relation(parentColumn = "sender_id", entityColumn = "id")
    val sender: UserEntity? = null,
)

//fun MessageEntry.asMessage() = MessageItem.MessageData(
//    id = message.externalId,
//    body = message.body,
//    timestamp = message.timestamp.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime(),
//    contactId = message.contactId,
//    senderId = sender?.id,
//    senderImage = null,
//    senderName = sender?.name,
//    ownerId = message.ownerId,
//    status = message.status,
//    read = message.read,
//    type = message.type,
//    attachment = message.getAttachment(),
//)

private fun MessageEntity.getAttachment() = when {
    image != null -> Attachment.Image(
        url = image.url,
        name = image.name,
        null, null, null, null
    )
    audio != null -> Attachment.Recording(
        url = audio.url,
        cacheUri = audio.cacheUri,
        duration = audio.duration,
        size = null, name = null
    )
    location != null -> Attachment.Location(lat = location.lat, lng = location.lng)
    file != null -> {
        if (file.type == File.Type.Pdf) Attachment.Pdf(
            url = file.url,
            name = file.name,
            cacheUri = file.cacheUri,
            size = null
        )
        else null
    }
    else -> null
}