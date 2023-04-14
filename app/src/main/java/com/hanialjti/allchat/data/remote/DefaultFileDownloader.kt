package com.hanialjti.allchat.data.remote

import com.hanialjti.allchat.data.remote.model.DownloadProgress
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class DefaultFileDownloader(
    private val httpClient: HttpClient = HttpClient(OkHttp) {
        install(Logging)
    },
    private val externalScope: CoroutineScope
): FileDownloader {

    private val _filesDownloadProgress = MutableStateFlow<MutableMap<Any, DownloadProgress>>(
        mutableMapOf()
    )
    override val filesDownloadProgress: SharedFlow<MutableMap<Any, DownloadProgress>> get() = _filesDownloadProgress

    override suspend fun download(url: String, identifier: Any): ByteArray {
        var totalLength: Long = 0
        val response = httpClient.get(url) {
            onDownload { bytesSentTotal, contentLength ->
                if (totalLength == 0L) {
                    totalLength = contentLength
                }
                updateDownloadProgress(
                    identifier,
                    DownloadProgress(
                        downloadedBytes = bytesSentTotal,
                        totalBytes = contentLength,
                        isFullyDownloaded = false,
                        downloaded = null
                    )
                )
            }
        }
        // TODO handle not enough space
        val downloadedBytes = response.readBytes()
        updateDownloadProgress(
            identifier,
            DownloadProgress(
                downloadedBytes = totalLength,
                totalBytes = totalLength,
                isFullyDownloaded = true,
                downloaded = downloadedBytes
            )
        )
        return downloadedBytes
    }

    private fun updateDownloadProgress(identifier: Any, downloadProgress: DownloadProgress) {
        externalScope.launch {
            _filesDownloadProgress.emit(
                _filesDownloadProgress.value.toMutableMap().apply {
                    this[identifier] = downloadProgress
                }
            )
        }
    }

}