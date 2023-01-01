package com.hanialjti.allchat.data.model

import com.hanialjti.allchat.common.exception.NotSupportedException
import com.hanialjti.allchat.presentation.chat.Attachment
import com.hanialjti.allchat.presentation.chat.defaultAttachmentName

data class Media(
    val id: Long = 0,
    val url: String? = null,
    val displayName: String? = null,
    val cacheUri: String? = null,
    val mimeType: String? = null,
    val duration: Long? = null,
    val type: Type? = null,
    val size: Int? = null,
    val width: Int? = null,
    val height: Int? = null,
) {
    enum class Type {
        Video,
        Image,
        Audio,
        Pdf,
        Other
    }
}

fun Media.asAttachment() = when (type) {
    Media.Type.Image -> Attachment.Image(
        url, displayName ?: defaultAttachmentName, cacheUri, size, width, height
    )
    Media.Type.Audio -> Attachment.Recording(
        url, displayName ?: defaultAttachmentName, cacheUri, size, duration?.toInt() ?: 0
    )
    Media.Type.Pdf -> Attachment.Pdf(url, displayName ?: defaultAttachmentName, cacheUri, size)
    else -> throw NotSupportedException("Other Attachment types are not supported for now!")
}