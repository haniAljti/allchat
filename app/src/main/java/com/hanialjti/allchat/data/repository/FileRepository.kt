package com.hanialjti.allchat.data.repository

import android.net.Uri
import com.hanialjti.allchat.common.utils.currentTimestamp
import com.hanialjti.allchat.data.model.FileMetadata
import com.hanialjti.allchat.data.remote.model.CallResult
import com.hanialjti.allchat.data.remote.model.DownloadProgress
import com.hanialjti.allchat.data.remote.model.UploadProgress
import kotlinx.coroutines.flow.SharedFlow
import okio.Path
import java.io.File

interface FileRepository {
    fun metadataOrNull(path: Path): FileMetadata?
    fun createNewAvatarFile(fileName: String): File
    suspend fun downloadAndSaveToInternalStorage(bytes: ByteArray, file: File): Uri?
    suspend fun downloadAndSaveToInternalStorage(url: String, subPath: String): Uri
    suspend fun downloadAndSaveToInternalStorage(url: String, file: File): Uri

    suspend fun downloadAndSaveToSharedStorage(
        url: String,
        downloadProgressId: Any = url
    ): String?

    fun createNewTempFile(fileExtension: String, name: String = "AC_$currentTimestamp"): File?
    fun deleteTempFile(file: File)
    fun saveFile(path: Path, mimeType: String? = null, fileName: String? = null): String?
    fun guessFileName(url: String, mimeType: String? = null): String?
    fun getUploadProgressForAll(): SharedFlow<MutableMap<Any, UploadProgress>>
    fun getDownloadProgressForAll(): SharedFlow<MutableMap<Any, DownloadProgress>>
    suspend fun uploadFile(file: File): CallResult<String>
}