package com.hanialjti.allchat.models

import com.hanialjti.allchat.exception.NotSupportedException
import com.hanialjti.allchat.models.entity.*
import com.hanialjti.allchat.utils.currentTimestamp

data class UiMessage(
    val id: Int,
    val body: String? = null,
    val timestamp: Long = currentTimestamp,
    val from: String? = null,
    val status: Status? = null,
    val readBy: Set<String>? = setOf(),
    val type: Type? = null,
    val attachment: Attachment? = null
)

sealed class Attachment(
    private val url: String? = null,
    private val name: String? = null,
    private val cacheUri: String? = null,
    private val mimeType: String? = null,
    private val duration: Int? = null,         // duration in ms
    private val type: Media.Type? = null,
    private val size: Int? = null,
    private val width: Int? = null,
    private val height: Int? = null,
    private val lat: Double? = null,
    private val lng: Double? = null,
) {

    class Image(
        val url: String?,
        val name: String,
        val cacheUri: String?,
        val size: Int?,
        val width: Int?,
        val height: Int?
    ) : Attachment(
        url = url,
        name = name,
        cacheUri = cacheUri,
        mimeType = "image/png",
        type = Media.Type.Image,
        size = size,
        width = width,
        height = height
    )

    class Recording(
        val url: String?,
        val name: String,
        val cacheUri: String?,
        val size: Int?,
        val duration: Int
    ) : Attachment(
        url = url,
        name = name,
        cacheUri = cacheUri,
        mimeType = "audio/mp4",
        duration = duration,
        type = Media.Type.Audio,
        size = size,
    )

    class Pdf(
        val url: String?,
        val name: String,
        val cacheUri: String?,
        val size: Int?,
    ) : Attachment(
        url = url,
        name = name,
        cacheUri = cacheUri,
        mimeType = "application/pdf",
        type = Media.Type.Pdf,
        size = size,
    )

    class Location(
        val lat: Double?,
        val lng: Double?
    ): Attachment(
        lat = lat,
        lng = lng
    )

    fun asMedia() = Media(url, name, cacheUri, mimeType, duration?.toLong(), type, size)

    fun asDownloadableAttachment() = when (this) {
        is Image -> url?.let { DownloadableAttachment(it, defaultAttachmentName, ".png") }
        is Recording -> url?.let { DownloadableAttachment(it, defaultAttachmentName, ".mp4") }
        is Pdf -> url?.let { DownloadableAttachment(it, defaultAttachmentName, ".pdf") }
        else -> throw NotSupportedException("Other Attachment types are not downloadable.")
    } ?: throw NotSupportedException("Not a downloadable attachment!")
}

data class DownloadableAttachment(val url: String, val name: String, val extension: String)

val defaultAttachmentName = "AC_$currentTimestamp"

fun Media.asAttachment() = when (type) {
    Media.Type.Image -> Attachment.Image(
        url, name ?: defaultAttachmentName, cacheUri, size, width, height)
    Media.Type.Audio -> Attachment.Recording(
        url, name ?: defaultAttachmentName, cacheUri, size, duration?.toInt() ?: 0
    )
    Media.Type.Pdf -> Attachment.Pdf(url, name ?: defaultAttachmentName, cacheUri, size)
    else -> throw NotSupportedException("Other Attachment types are not supported for now!")
}

fun Location.asAttachment() = Attachment.Location(lat, lng)


