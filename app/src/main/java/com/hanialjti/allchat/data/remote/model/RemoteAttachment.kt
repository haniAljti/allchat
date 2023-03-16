package com.hanialjti.allchat.data.remote.model

import com.hanialjti.allchat.common.utils.FileUtils
import com.hanialjti.allchat.data.model.Attachment

sealed interface RemoteAttachment {
    fun toAttachment(): Attachment
}

data class Media(
    val url: String,
    val desc: String?
) : RemoteAttachment {
    override fun toAttachment(): Attachment {
        val metadata = FileUtils.metadataOrNull(url)
        return com.hanialjti.allchat.data.model.Media(
            type = Attachment.Type.fromMimeType(metadata?.mimeType),
            url = url,
            cacheUri = null,
            fileName = metadata?.displayName,
            mimeType = metadata?.mimeType
        )
    }
}

data class Location(
    val lat: Double,
    val lng: Double
) : RemoteAttachment {
    override fun toAttachment() = com.hanialjti.allchat.data.model.Location(lat, lng)
}