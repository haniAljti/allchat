package com.hanialjti.allchat.data.remote

import com.hanialjti.allchat.data.remote.model.CallResult
import com.hanialjti.allchat.data.remote.model.UploadProgress
import kotlinx.coroutines.flow.SharedFlow
import java.io.File

interface FileUploader {
    val filesUploadProgress: SharedFlow<MutableMap<Any, UploadProgress>>
    suspend fun upload(file: File, identifier: Any = file): CallResult<String>
}