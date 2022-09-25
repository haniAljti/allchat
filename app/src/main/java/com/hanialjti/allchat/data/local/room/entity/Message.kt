package com.hanialjti.allchat.data.local.room.entity

import androidx.room.*
import com.hanialjti.allchat.common.utils.currentTimestamp

@Entity
@TypeConverters(MessageStatusConverter::class)
data class Message(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "messageId")
    val id: Int = 0,
    val remoteId: String? = null,
    val body: String? = null,
    val timestamp: Long = currentTimestamp,
    val conversation: String? = null,
    val from: String? = null,      //sent by
    val owner: String? = null,     //currently signed in user
    val status: Status? = Status.Pending,
    val readBy: Set<String>? = setOf(),
    val type: Type? = null,
    @Embedded
    val media: Media? = null,
    @Embedded
    val location: Location? = null
)

fun Message.toErrorStatusMessage() = StatusMessage(
    id = id,
    remoteId = remoteId,
    timestamp = timestamp,
    status = Status.Error,
    from = from,
    owner = owner,
    conversation = conversation
)


enum class Status(val value: Int) {
    Pending(0),
    Error(1),
    Sending(2),
    Sent(3),
    Acknowledged(3),
    Received(4),
    Seen(5),
}

enum class DeliveryStatus(val value: Int) {
    Delivered(0),
    Seen(1)
}

enum class Type { Chat, GroupChat }

@TypeConverters(MessageStatusConverter::class)
data class StatusMessage(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "messageId")
    val id: Int = 0,
    val remoteId: String? = null,
    val timestamp: Long,
    val status: Status,
    val from: String?,
    val owner: String?,
    val conversation: String?
)

@TypeConverters(MessageStatusConverter::class)
data class UpdateMessage(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "messageId")
    val id: Int = 0,
    val remoteId: String? = null,
    val body: String? = null,
    val timestamp: Long = currentTimestamp,
    val conversation: String? = null,
    val from: String? = null,
    val owner: String? = null,
    val readBy: Set<String>? = setOf(),
    val type: Type? = null,
    @Embedded
    val media: Media? = null,
    @Embedded
    val location: Location? = null
)

fun Message.toUpdateMessage() =
    UpdateMessage(id, remoteId, body, timestamp, conversation, from, owner, readBy, type, media, location)

fun Status?.hasLesserValueThan(otherStatus: Status) = (this?.ordinal ?: 0) < otherStatus.ordinal

data class Location(
    val lat: Double,
    val lng: Double
)

data class Media(
    @ColumnInfo(name = "mediaUrl")
    val url: String? = null,
    @ColumnInfo(name = "mediaDisplayName")
    val name: String? = null,
    @ColumnInfo(name = "mediaCacheUri")
    val cacheUri: String? = null,
    @ColumnInfo(name = "mediaMimeType")
    val mimeType: String? = null,
    @ColumnInfo(name = "mediaDuration")
    val duration: Long? = null,
    @ColumnInfo(name = "mediaType")
    val type: Type? = null,
    @ColumnInfo(name = "mediaSize")
    val size: Int? = null,
    @ColumnInfo(name = "mediaWidth")
    val width: Int? = null,
    @ColumnInfo(name = "mediaHeight")
    val height: Int? = null,
) {
    enum class Type { Video, Image, Audio, Pdf, Other }
}