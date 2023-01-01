package com.hanialjti.allchat.presentation.chat

import com.hanialjti.allchat.common.exception.NotSupportedException
import com.hanialjti.allchat.common.utils.currentTimestamp
import com.hanialjti.allchat.data.local.room.entity.AttachmentEntity

sealed class Attachment(
    val url: String? = null,
    val displayName: String? = null,
    val cacheUri: String? = null,
    val mimeType: String? = null,
    val duration: Long? = null,
    val type: Type? = null,
    val size: Int? = null,
    val width: Int? = null,
    val height: Int? = null,
    val lat: Double? = null,
    val lng: Double? = null
) {

    class Image(
        url: String?,
        name: String?,
        cacheUri: String?,
        size: Int?,
        width: Int?,
        height: Int?
    ) : Attachment(
        url = url,
        displayName = name,
        cacheUri = cacheUri,
        mimeType = "image/png",
        type = Type.Image,
        size = size,
        width = width,
        height = height
    )

    class Recording(
        url: String?,
        name: String?,
        cacheUri: String?,
        size: Int?,
        duration: Int?
    ) : Attachment(
        url = url,
        displayName = name,
        cacheUri = cacheUri,
        mimeType = "audio/mp4",
        type = Type.Audio,
        size = size,
        duration = duration?.toLong()
    )

    class Pdf(
        url: String?,
        name: String?,
        cacheUri: String?,
        size: Int?,
    ) : Attachment(
        url = url,
        displayName = name,
        cacheUri = cacheUri,
        mimeType = "application/pdf",
        type = Type.Pdf,
        size = size
    )

    class Location(
        lat: Double?,
        lng: Double?
    ) : Attachment(
        lat = lat,
        lng = lng,
        type = Type.Location
    )

    fun asAttachmentEntity(): AttachmentEntity = AttachmentEntity(
        url = url,
        displayName = displayName,
        cacheUri = cacheUri,
        mimeType = mimeType,
        type = type,
        size = size,
        duration = duration,
        width = width,
        height = height,
        lat = lat,
        lng = lng
    )

    fun asDownloadableAttachment() = when (this) {
        is Image -> url?.let { DownloadableAttachment(it, defaultAttachmentName, ".png") }
        is Recording -> url?.let { DownloadableAttachment(it, defaultAttachmentName, ".mp4") }
        is Pdf -> url?.let { DownloadableAttachment(it, defaultAttachmentName, ".pdf") }
        else -> throw NotSupportedException("Other Attachment types are not downloadable.")
    } ?: throw NotSupportedException("Not a downloadable attachment!")

    enum class Type {
        Video,
        Image,
        Audio,
        Pdf,
        Location,
        Other
    }
}

data class DownloadableAttachment(val url: String, val name: String, val extension: String)

val defaultAttachmentName = "AC_$currentTimestamp"