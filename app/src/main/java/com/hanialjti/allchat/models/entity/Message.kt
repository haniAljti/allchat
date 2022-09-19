package com.hanialjti.allchat.models.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hanialjti.allchat.utils.currentTimestamp
import java.util.*

@Entity
data class Message(
    @PrimaryKey
    @ColumnInfo(name = "messageId")
    val id: String = UUID.randomUUID().toString(),
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

enum class Status { Pending, Sent, Seen, Received, Error }
enum class Type { Chat, GroupChat }

data class StatusMessage(
    @ColumnInfo(name = "messageId")
    val id: String,
    val timestamp: Long,
    val status: Status,
    val from: String?,
    val owner: String?,
    val conversation: String?
)

data class UpdateMessage(
    @ColumnInfo(name = "messageId")
    val id: String = UUID.randomUUID().toString(),
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
    UpdateMessage(id, body, timestamp, conversation, from, owner, readBy, type, media, location)

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