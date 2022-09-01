package com.hanialjti.allchat.models.entity

import androidx.room.*
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
    val from: String? = null,
    val status: String? = null,
    val readBy: Set<String> = setOf(),
    val type: String? = null,
    @Embedded
    val media: Media? = null,
    @Embedded
    val location: Location? = null
)

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