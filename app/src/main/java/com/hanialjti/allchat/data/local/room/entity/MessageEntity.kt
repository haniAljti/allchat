package com.hanialjti.allchat.data.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hanialjti.allchat.data.model.MessageItem
import com.hanialjti.allchat.data.model.MessageStatus
import com.hanialjti.allchat.data.model.MessageType
import com.hanialjti.allchat.data.remote.model.RemoteMessage
import com.hanialjti.allchat.presentation.chat.Attachment
import com.hanialjti.allchat.presentation.conversation.ContactImage
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.serialization.Serializable
import java.time.Clock
import java.time.OffsetDateTime
import java.time.ZoneId

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "message_id")
    val id: Long = 0,
    @ColumnInfo(name = "external_id")
    val externalId: String? = null,
    val body: String? = null,
    val timestamp: OffsetDateTime = OffsetDateTime.now(),
    @ColumnInfo(name = "contact_id")
    val contactId: String? = null,
    @ColumnInfo(name = "sender_id")
    val senderId: String? = null,
    @ColumnInfo(name = "sender_image")
    val senderName: String? = null,
    @ColumnInfo(name = "sender_name")
    val senderImage: String? = null,
    @ColumnInfo(name = "owner_id")
    val ownerId: String? = null,
    val status: MessageStatus = MessageStatus.Pending,
    val read: Boolean = false,
    val type: MessageType? = null,
    @ColumnInfo(name = "message_archive_id")
    val archiveId: String? = null,
    val image: ImageAttachment? = null,
    val audio: AudioAttachment? = null,
    val file: FileAttachment? = null,
    val location: LocationAttachment? = null
)

@Serializable
data class ImageAttachment(
    val url: String,
    val name: String?,
)

@Serializable
data class AudioAttachment(
    val url: String,
    val cacheUri: String?,
    val duration: Int?
)

@Serializable
data class LocationAttachment(
    val lat: Double,
    val lng: Double,
)

@Serializable
data class FileAttachment(
    val url: String,
    val name: String,
    val cacheUri: String,
    val type: File.Type
)

object File { enum class Type { File, Pdf } }

fun MessageEntity.asNetworkMessage() = externalId?.let {
    RemoteMessage(
        id = externalId,
        body = body,
        timestamp = timestamp,
        chatId = contactId,
        sender = senderId,
        messageStatus = status,
        type = type,
        messageArchiveId = archiveId
    )
}

fun MessageStatus?.hasLesserValueThan(otherStatus: MessageStatus?) =
    (this?.value ?: 0) < (otherStatus?.value ?: 0)

fun MessageEntity.asMessage() = MessageItem.MessageData(
    id = externalId,
    body = body,
    timestamp = timestamp.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime().toKotlinLocalDateTime(),
    contactId = contactId,
    senderId = senderId,
    senderImage = senderImage?.let { ContactImage.DynamicImage(senderImage) }
        ?: ContactImage.DefaultProfileImage(type == MessageType.GroupChat),
    senderName = senderName,
    ownerId = ownerId,
    status = status,
    read = read,
    type = type,
    attachment = getAttachment(),
)

fun MessageEntity.getAttachment() = when {
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