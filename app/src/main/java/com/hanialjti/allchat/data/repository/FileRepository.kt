package com.hanialjti.allchat.data.repository

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.TextUtils
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import androidx.annotation.RequiresApi
import com.google.modernstorage.storage.AndroidFileSystem
import com.google.modernstorage.storage.toOkioPath
import com.google.modernstorage.storage.toUri
import com.hanialjti.allchat.common.utils.Logger
import com.hanialjti.allchat.common.utils.currentTimestamp
import com.hanialjti.allchat.common.utils.sdkEqualsOrUp
import com.hanialjti.allchat.data.model.FileMetadata
import com.hanialjti.allchat.data.remote.DefaultFileDownloader
import com.hanialjti.allchat.data.remote.xmpp.FileXmppDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okio.Path
import okio.Path.Companion.toOkioPath
import timber.log.Timber
import java.io.*
import java.util.*


class FileRepository(
    context: Context,
    private val dispatcher: CoroutineDispatcher,
    private val remoteDataSource: FileXmppDataSource,
    private val fileDownloader: DefaultFileDownloader,
    private val mediaSubFolderDir: String = "AllChat"
) {

    private val cacheDir = context.cacheDir
    private val cr = context.contentResolver

    private val fileSystem = AndroidFileSystem(context)

    companion object {
        private const val NO_MIME_TYPE = "*/*"
        private const val MIME_TYPE_IMAGE = "image/"
        private const val MIME_TYPE_VIDEO = "video/"
        private const val MIME_TYPE_AUDIO = "audio/"
        private const val MIME_TYPE_DOCUMENT = "application/"

        private const val AVATAR_DIR = "/avatars"
    }

    fun metadataOrNull(path: Path): FileMetadata? {

        val uri = path.toUri()

        return when (uri.authority) {
            null, "file" -> fetchMetadataFromPhysicalFile(path)
            MediaStore.AUTHORITY -> fetchMetadataFromMediaStore(path)
            else -> fetchMetadataFromDocumentProvider(path)
        }
    }

    fun metadataOrNull(url: String): FileMetadata? {

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            Timber.e("the url passed is not a valid one: $url")
            return null
        }
        val fileExtension = MimeTypeMap.getFileExtensionFromUrl(url)
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.lowercase(Locale.getDefault()))
        val fileName = guessFileName(url, mimeType)

        return FileMetadata(
            size = 0,
            displayName = fileName,
            mimeType = mimeType
        )
    }

    private fun fetchMetadataFromPhysicalFile(path: Path): FileMetadata? {

        val file = path.toFile()
        val isRegularFile = file.isFile
        val isDirectory = file.isDirectory
        val lastModifiedAtMillis = file.lastModified()
        val size = file.length()

        if (!isRegularFile &&
            !isDirectory &&
            lastModifiedAtMillis == 0L &&
            size == 0L &&
            !file.exists()
        ) {
            return null
        }

        val fileExtension: String = MimeTypeMap.getFileExtensionFromUrl(file.toString())
        val mimeType = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(fileExtension.lowercase(Locale.getDefault()))

        return FileMetadata(
            size = size,
            displayName = file.name,
            mimeType = mimeType
        )
    }

    private fun fetchMetadataFromMediaStore(path: Path): FileMetadata? {

        val uri = path.toUri()

        if (uri.pathSegments.firstOrNull().isNullOrBlank()) {
            return null
        }

        val cursor = cr.query(
            uri,
            arrayOf(
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.SIZE,
            ),
            null,
            null,
            null
        ) ?: return null

        cursor.use { cursor ->
            if (!cursor.moveToNext()) {
                return null
            }

            val displayName =
                cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME))
            val mimeType =
                cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE))
            val size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE))

            return FileMetadata(
                size = size,
                displayName = displayName,
                mimeType = mimeType
            )
        }
    }

    private fun fetchMetadataFromDocumentProvider(path: Path): FileMetadata? {
        val uri = path.toUri()
        val cursor = cr.query(
            uri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE
            ),
            null,
            null,
            null
        ) ?: return null

        cursor.use { cursor ->
            if (!cursor.moveToNext()) {
                return null
            }

            val displayName = cursor.getString(0)
            val mimeType = cursor.getString(1)
            val size = cursor.getLong(2)

            return FileMetadata(
                size = size,
                displayName = displayName,
                mimeType = mimeType
            )
        }
    }

    private val avatarDirFile = File(cacheDir, AVATAR_DIR)
    private fun createAvatarsSubDir(): File {
        val theDir = avatarDirFile
        if (!theDir.exists()) {
            theDir.mkdirs()
        }
        return theDir
    }

    val avatarDir get() = createAvatarsSubDir()

    fun createNewAvatarFile(fileName: String) = File(avatarDir, fileName)

    suspend fun downloadAndSaveToInternalStorage(bytes: ByteArray, subPath: String): Uri? {
        return downloadAndSaveToInternalStorage(bytes, File(cacheDir, subPath))
    }

    suspend fun downloadAndSaveToInternalStorage(bytes: ByteArray, file: File): Uri? =
        withContext(dispatcher) {
            return@withContext try {
                fileSystem.write(file.toOkioPath()) { write(bytes) }
                Uri.fromFile(file)
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }


    @SuppressLint("Range")
    fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme.equals("content")) {
            val cursor = cr.query(uri, null, null, null, null)

            cursor?.use {
                if (it.moveToFirst()) {
                    result = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                result = cut?.plus(1)?.let { result?.substring(it) }
            }
        }
        return result

    }

    suspend fun downloadAndSaveToInternalStorage(url: String, subPath: String): Uri {
        return downloadAndSaveToInternalStorage(url, File(cacheDir, subPath))
    }

    suspend fun downloadAndSaveToInternalStorage(url: String, file: File): Uri =
        withContext(dispatcher) {
            return@withContext try {
                val downloadResponse = fileDownloader.download(url)
                fileSystem.write(file.toOkioPath()) {
                    write(downloadResponse)
                }
                Uri.fromFile(file)
            } catch (e: IOException) {
                e.printStackTrace()
                throw IOException("Error while creating image")
            }
        }

    suspend fun downloadAndSaveToSharedStorage(url: String, downloadProgressId: Any = url): String? = withContext(dispatcher) {

        val mimeType = getMimeTypeFromUrl(url)

        var relativeLocation: String = appropriateDir(mimeType)

        if (!TextUtils.isEmpty(mediaSubFolderDir)) {
            relativeLocation += File.separator + mediaSubFolderDir
        }

        val mediaCollection = sdkEqualsOrUp(Build.VERSION_CODES.Q) { getUri(mimeType) }
            ?: MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val mediaDetails = ContentValues().apply {
            put(MediaStore.Files.FileColumns.DISPLAY_NAME, guessFileName(url, mimeType))
            sdkEqualsOrUp(Build.VERSION_CODES.Q) {
                put(MediaStore.Files.FileColumns.IS_PENDING, 1)
                put(MediaStore.Files.FileColumns.RELATIVE_PATH, relativeLocation)
            }
            put(MediaStore.Files.FileColumns.MIME_TYPE, mimeType)
        }

        val mediaContentUri = cr.insert(mediaCollection, mediaDetails)
            ?: return@withContext null

        try {
            val downloadResponse = fileDownloader.download(url, downloadProgressId)
            fileSystem.write(mediaContentUri.toOkioPath()) {
                write(downloadResponse)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaDetails.clear()
            mediaDetails.put(MediaStore.Files.FileColumns.IS_PENDING, 0)
            cr.update(mediaContentUri, mediaDetails, null, null)
        }

        return@withContext getRealPathFromURI(mediaContentUri)
    }


    private fun getExtension(fileName: String) =
        fileName.lastIndexOf(".")
            .takeIf { it > 0 }
            .let { fileName.substring(it ?: 0) }
            .ifBlank { null }

    private fun getMimeType(fileExtension: String): String? {
        val mimeTypeMap = MimeTypeMap.getSingleton() ?: return null
        val extension = fileExtension.lowercase()
        return if (mimeTypeMap.hasMimeType(extension))
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        else NO_MIME_TYPE
    }


    /**
     * @return new file path
     */
    fun createNewTempFile(name: String = generateTempName(), fileExtension: String): File? {
        val tempFile = try {
            val tempFile = File(cacheDir, name.plus(fileExtension))
            tempFile.createNewFile()
            tempFile
        } catch (e: IOException) {
            Logger.e(e)
            null
        } catch (e: SecurityException) {
            Logger.e(e)
            null
        }
        return tempFile
    }

    fun deleteTempFile(file: File) {
        try {
            file.delete()
        } catch (e: SecurityException) {
            Logger.e(e)
        }
    }

    private fun generateTempName() = "AC_$currentTimestamp"

    fun saveFile(path: Path, mimeType: String? = null, fileName: String? = null): String? {

        val metadata = if (mimeType == null && fileName == null) {
            metadataOrNull(path)
        } else null

        val extractedMimeType = mimeType ?: metadata?.mimeType
        val extractedFileName = fileName ?: metadata?.displayName

        var relativeLocation: String = appropriateDir(extractedMimeType)

        if (!TextUtils.isEmpty(mediaSubFolderDir)) {
            relativeLocation += File.separator.plus(mediaSubFolderDir)
        }

        val mediaCollection = sdkEqualsOrUp(Build.VERSION_CODES.Q) { getUri(extractedMimeType) }
            ?: MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val mediaDetails = ContentValues().apply {
            put(
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                extractedFileName ?: defaultAttachmentName
            )
            sdkEqualsOrUp(Build.VERSION_CODES.Q) {
                put(MediaStore.Files.FileColumns.IS_PENDING, 1)
                put(MediaStore.Files.FileColumns.RELATIVE_PATH, relativeLocation)
            }
            put(MediaStore.Files.FileColumns.MIME_TYPE, extractedMimeType)
        }

        val mediaContentUri = cr.insert(mediaCollection, mediaDetails) ?: return null
        val newFileUri = mediaContentUri.toOkioPath()

        fileSystem.copy(path, newFileUri)

        mediaDetails.clear()
        mediaDetails.put(MediaStore.Files.FileColumns.IS_PENDING, 0)
        cr.update(mediaContentUri, mediaDetails, null, null)

        return getRealPathFromURI(mediaContentUri)
    }

    fun getMimeTypeFromUrl(url: String): String? {
        return guessFileName(url)
            ?.let { fileName ->
                getExtension(fileName)?.let { mimeType ->
                    getMimeType(mimeType)
                }
            }
    }

    fun guessFileName(url: String, mimeType: String? = null): String? {
        return URLUtil.guessFileName(url, null, mimeType)
    }

    private fun appropriateDir(mimeType: String?) = when {
        mimeType == null || mimeType == NO_MIME_TYPE -> Environment.DIRECTORY_DOWNLOADS
        isImage(mimeType) || isVideo(mimeType) -> Environment.DIRECTORY_DCIM
        isAudio(mimeType) -> sdkEqualsOrUp(Build.VERSION_CODES.S) { Environment.DIRECTORY_RECORDINGS }
            ?: Environment.DIRECTORY_MUSIC
        isDocument(mimeType) -> Environment.DIRECTORY_DOCUMENTS
        else -> Environment.DIRECTORY_DOWNLOADS
    }

    private fun isImage(mimeType: String?) = mimeType?.contains(MIME_TYPE_IMAGE) ?: false
    private fun isVideo(mimeType: String?) = mimeType?.contains(MIME_TYPE_VIDEO) ?: false
    private fun isAudio(mimeType: String?) = mimeType?.contains(MIME_TYPE_AUDIO) ?: false
    private fun isDocument(mimeType: String?) = mimeType?.contains(MIME_TYPE_DOCUMENT) ?: false

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getUri(mimeType: String?, volume: String = MediaStore.VOLUME_EXTERNAL_PRIMARY) =
        when {
            isImage(mimeType) -> MediaStore.Images.Media.getContentUri(volume)
            isVideo(mimeType) -> MediaStore.Video.Media.getContentUri(volume)
            isAudio(mimeType) -> MediaStore.Audio.Media.getContentUri(volume)
            else -> MediaStore.Files.getContentUri(volume)
        }


    private fun getRealPathFromURI(contentUri: Uri): String? {
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        return try {
            cr.query(contentUri, proj, null, null, null)
                ?.use { cursor ->
                    val columnIndex: Int =
                        cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    cursor.run {
                        moveToFirst()
                        getString(columnIndex)
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getUploadProgressForAll() = remoteDataSource.filesUploadProgress
    fun getDownloadProgressForAll() = fileDownloader.filesDownloadProgress

    fun uploadFile(file: File) = remoteDataSource.upload(file)

}

val defaultAttachmentName get() = "AC_$currentTimestamp"