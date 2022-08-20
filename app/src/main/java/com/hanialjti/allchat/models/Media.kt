package com.hanialjti.allchat.models

data class Media(
    val url: String? = null,
    val cacheUri: String? = null,
    val mimeType: String? = null,
    val duration: Long? = null,
    val type: Type? = null,
    val size: Int? = null,
) {
    enum class Type { Video, Image, Audio, Pdf, Other }
}
