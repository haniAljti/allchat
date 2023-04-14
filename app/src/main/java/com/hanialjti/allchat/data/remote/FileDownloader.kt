package com.hanialjti.allchat.data.remote

import com.hanialjti.allchat.data.remote.model.DownloadProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface FileDownloader {
    val filesDownloadProgress: SharedFlow<MutableMap<Any, DownloadProgress>>
    suspend fun download(url: String, identifier: Any = url): ByteArray
}