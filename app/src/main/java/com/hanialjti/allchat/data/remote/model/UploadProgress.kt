package com.hanialjti.allchat.data.remote.model

data class UploadProgress(
    val uploadedBytes: Long,
    val totalBytes: Long,
    val isUploaded: Boolean,
    val url: String?
) {
    override fun toString(): String {
        return "UploadProgress(uploadedBytes=$uploadedBytes, totalBytes=$totalBytes, isUploaded=$isUploaded, url=$url)"
    }
}