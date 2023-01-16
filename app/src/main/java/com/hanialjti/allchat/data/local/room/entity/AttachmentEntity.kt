package com.hanialjti.allchat.data.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hanialjti.allchat.presentation.chat.Attachment

data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "url")
    val url: String? = null,
    @ColumnInfo(name = "display_name")
    val displayName: String? = null,
    @ColumnInfo(name = "cache_url")
    val cacheUri: String? = null,
    @ColumnInfo(name = "mime_type")
    val mimeType: String? = null,
    @ColumnInfo(name = "duration")
    val duration: Long? = null,
    @ColumnInfo(name = "type")
    val type: Attachment.Type? = null,
    @ColumnInfo(name = "size")
    val size: Int? = null,
    @ColumnInfo(name = "width")
    val width: Int? = null,
    @ColumnInfo(name = "height")
    val height: Int? = null,
    val lat: Double? = null,
    val lng: Double? = null
)

fun AttachmentEntity.asAttachment() = when(type) {
    Attachment.Type.Image -> Attachment.Image(
        url = url,
        name = displayName,
        cacheUri = cacheUri,
        size = size,
        width = width,
        height = height
    )
    Attachment.Type.Audio -> Attachment.Recording(
        url = url,
        name = displayName,
        cacheUri = cacheUri,
        size = size,
        duration = duration?.toInt()
    )
    Attachment.Type.Pdf -> Attachment.Pdf(
        url = url,
        name = displayName,
        cacheUri = cacheUri,
        size = size
    )
    Attachment.Type.Location -> Attachment.Location(
        lat = lat,
        lng = lng
    )
    else -> { null }
}
