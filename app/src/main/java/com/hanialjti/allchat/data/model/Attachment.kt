package com.hanialjti.allchat.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
sealed class Attachment {

    abstract val type: Type

    enum class Type(val value: Int) {
        Video(0),
        Image(1),
        Audio(2),
        Document(3),
        File(4),
        Location(5);

        companion object {
            fun fromValue(value: Int) = when (value) {
                Video.value -> Video
                Image.value -> Image
                Audio.value -> Audio
                Document.value -> Document
                File.value -> File
                else -> Location
            }

            fun fromMimeType(mimeType: String?) = when  {
                mimeType == null -> File
                mimeType.contains("image") -> Image
                mimeType.contains("audio") -> Audio
                mimeType.contains("application") -> Document
                mimeType.contains("video") -> Video
                else -> File
            }
        }
    }

}

@Serializable
data class Media(
    override val type: Type,
    val url: String? = null,
    val cacheUri: String?,
    val fileName: String? = null,
    val duration: Long? = null,
    val mimeType: String? = null
) : Attachment()

@Serializable
data class Location(
    val lat: Double,
    val lng: Double,
) : Attachment() {
    override val type: Type
        get() = Type.Location
}

object AttachmentSerializer : JsonContentPolymorphicSerializer<Attachment>(Attachment::class) {
    override fun selectDeserializer(element: JsonElement) = when {
        "type" in element.jsonObject -> {
            val sourceContent = element.jsonObject["type"]?.jsonPrimitive?.contentOrNull

            when (
                Attachment.Type.values().firstOrNull { it.name == sourceContent }
            ) {
                Attachment.Type.Location -> Location.serializer()
                else -> Media.serializer()
            }
        }
        else -> error("no 'type' in JSON")
    }
}