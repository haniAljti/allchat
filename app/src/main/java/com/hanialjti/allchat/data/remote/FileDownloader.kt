package com.hanialjti.allchat.data.remote

import com.hanialjti.allchat.data.remote.model.DownloadProgress
import kotlinx.coroutines.flow.Flow

interface FileDownloader {
    suspend fun download(url: String): Flow<DownloadProgress>
}