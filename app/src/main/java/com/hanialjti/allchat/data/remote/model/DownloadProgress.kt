package com.hanialjti.allchat.data.remote.model

data class DownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long,
    val isFullyDownloaded: Boolean,
    val downloaded: ByteArray?
) {
    override fun toString(): String {
        return "UploadProgress(downloadedBytes=$downloadedBytes, totalBytes=$totalBytes, isDownloaded=$isFullyDownloaded)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DownloadProgress

        if (downloadedBytes != other.downloadedBytes) return false
        if (totalBytes != other.totalBytes) return false
        if (isFullyDownloaded != other.isFullyDownloaded) return false
        if (downloaded != null) {
            if (other.downloaded == null) return false
            if (!downloaded.contentEquals(other.downloaded)) return false
        } else if (other.downloaded != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = downloadedBytes.hashCode()
        result = 31 * result + totalBytes.hashCode()
        result = 31 * result + isFullyDownloaded.hashCode()
        result = 31 * result + (downloaded?.contentHashCode() ?: 0)
        return result
    }
}