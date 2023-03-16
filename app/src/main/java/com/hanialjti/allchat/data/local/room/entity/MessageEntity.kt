package com.hanialjti.allchat.data.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hanialjti.allchat.common.utils.dateAsString
import com.hanialjti.allchat.common.utils.timeAsString
import com.hanialjti.allchat.data.model.*
import com.hanialjti.allchat.data.remote.model.RemoteMessage
import com.hanialjti.allchat.presentation.conversation.ContactImage
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val body: String? = null,
    val timestamp: OffsetDateTime = OffsetDateTime.now(),
    @ColumnInfo(name = "contact_id")
    val contactId: String? = null,
    @ColumnInfo(name = "sender_id")
    val senderId: String? = null,
    @ColumnInfo(name = "sender_name")
    val senderName: String? = null,
    @ColumnInfo(name = "sender_image")
    val senderImage: String? = null,
    @ColumnInfo(name = "owner_id")
    val ownerId: String? = null,
    val status: MessageStatus = MessageStatus.Pending,
    val read: Boolean = false,
    val thread: String? = null,
    val type: MessageType? = null,
    @ColumnInfo(name = "message_archive_id")
    val archiveId: String? = null,
    val isMarkable: Boolean = false,
    val attachment: Attachment? = null,
    val markers: Map<String, MessageMarker> = mapOf()
)
@kotlinx.serialization.Serializable
data class MessageMarker(
    val marker: Marker,
    @kotlinx.serialization.Serializable(with = KOffsetDateTimeSerializer::class)
    val timestamp: OffsetDateTime = OffsetDateTime.now()
)

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = OffsetDateTime::class)
object KOffsetDateTimeSerializer : KSerializer<OffsetDateTime> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("OffsetDateTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: OffsetDateTime) {
        val format = DateTimeFormatter.ISO_OFFSET_DATE_TIME

        val string = format.format(value)
        encoder.encodeString(string)
    }

    override fun deserialize(decoder: Decoder): OffsetDateTime {
        val string = decoder.decodeString()
        return OffsetDateTime.parse(string)
    }
}

fun MessageEntity.asNetworkMessage() = RemoteMessage(
    id = id,
    body = body,
    timestamp = timestamp,
    chatId = contactId,
    sender = senderId,
    messageStatus = status,
    type = type,
    messageArchiveId = archiveId
)


fun MessageStatus?.hasLesserValueThan(otherStatus: MessageStatus?) =
    (this?.value ?: 0) < (otherStatus?.value ?: 0)

fun MessageEntity.asMessage() = MessageItem.MessageData(
    id = id,
    body = body,
    date = timestamp.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
        .dateAsString(),
    time = timestamp.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
        .timeAsString(),
    timestamp = timestamp.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
        .toKotlinLocalDateTime(),
    contactId = contactId,
    senderId = senderId,
    senderImage = senderImage?.let { ContactImage.DynamicImage(senderImage) }
        ?: ContactImage.DefaultProfileImage(type == MessageType.GroupChat),
    senderName = senderName,
    ownerId = ownerId,
    status = status,
    read = read,
    type = type,
    attachment = attachment,
)