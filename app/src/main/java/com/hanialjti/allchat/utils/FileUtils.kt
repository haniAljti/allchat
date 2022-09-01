package com.hanialjti.allchat.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.hanialjti.allchat.exception.NotSupportedException
import com.hanialjti.allchat.models.UiAttachment
import com.hanialjti.allchat.models.entity.Media
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL

private const val appName = "AllChat"
private val photosDir = Environment.DIRECTORY_PICTURES + File.separator + appName

private fun getMediaExtension(type: Media.Type) = when(type) {
    Media.Type.Image -> ".jpg"
    Media.Type.Audio -> ".acc"
    Media.Type.Pdf -> ".pdf"
    else -> throw NotSupportedException("Media type is not yet supported!")
}


suspend fun Context.saveBitmapToInternalStorage(bmp: Bitmap, name: String): Uri = withContext(Dispatchers.IO) {
    return@withContext try {

        val imageFile = File(cacheDir, "$name.jpg}")
        val fileOutputStream = FileOutputStream(imageFile)
        fileOutputStream.use { stream ->
            if (!bmp.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                throw IOException("Couldn't save bitmap.")
            }
        }
        Uri.fromFile(imageFile)
    } catch (e: IOException) {
        e.printStackTrace()
        throw IOException("Error while creating image")
    }
}

suspend fun Context.saveImageToInternalStorage(uri: Uri, name: String): Uri = withContext(Dispatchers.IO) {
    return@withContext try {

        val imageFile = File(cacheDir, "$name.jpg}")
        val fileOutputStream = FileOutputStream(imageFile)
        fileOutputStream.use { outputStream ->
            contentResolver.openInputStream(uri).use { inputStream ->
                inputStream?.copyTo(outputStream)
            }
        }
        Uri.fromFile(imageFile)
    } catch (e: IOException) {
        e.printStackTrace()
        throw IOException("Error while creating image")
    }
}

suspend fun Context.saveAttachmentToInternalStorage(attachment: UiAttachment): Uri = withContext(Dispatchers.IO) {
    return@withContext try {

        val downloadableAttachment = attachment.toDownloadableAttachment()

        val attachmentFile = File(cacheDir, downloadableAttachment.name + downloadableAttachment.extension)
        val fileOutputStream = FileOutputStream(attachmentFile)

        URL(downloadableAttachment.url)
            .openConnection()
            .getInputStream()
            .use { inputStream ->
                fileOutputStream.use { fileOutputStream ->
                    inputStream.copyTo(fileOutputStream)
                }
            }

        Uri.fromFile(attachmentFile)
    } catch (e: IOException) {
        e.printStackTrace()
        throw IOException("Error while creating file")
    }
}

fun Context.createFileInInternalStorage(name: String, type: Media.Type) =
    File(cacheDir, name + getMediaExtension(type))


suspend fun Context.saveMediaToExternalStorage(media: Media): Uri = withContext(Dispatchers.IO) {
    return@withContext try {

        val uri: Uri?

        val contentUri = getMediaContentUri(Media.Type.Image) ?: throw IOException("")

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.ImageColumns.DISPLAY_NAME, media.name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.ImageColumns.RELATIVE_PATH, photosDir)
            }
        }

        uri = contentResolver.insert(contentUri, contentValues)
            ?: throw IOException("Failed to create new MediaStore record.")

        URL(media.url)
            .openConnection()
            .getInputStream()
            .use {
                contentResolver.openOutputStream(uri)?.use { stream ->
                    it.copyTo(stream)
                }
            }

        uri
    } catch (e: IOException) {
        e.printStackTrace()
        throw IOException("Error while creating image ")
    }
}

fun Context.cacheCameraBitmap(bitmap: Bitmap, name: String): Uri? {

    val contentUri = getMediaContentUri(Media.Type.Image) ?: return null

    val contentValues = ContentValues().apply {
        put(MediaStore.Images.ImageColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.ImageColumns.RELATIVE_PATH, photosDir)
        }
    }

    var uri: Uri? = null

    try {
        uri = contentResolver.insert(contentUri, contentValues)
            ?: throw IOException("Failed to create new MediaStore record.")

        contentResolver.openOutputStream(uri)?.use { stream ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                throw IOException("Failed to save bitmap.")
            }
        } ?: throw IOException("Failed to get output stream.")

        return uri

    } catch (e: IOException) {
        if (uri != null) {
            contentResolver.delete(uri, null, null)
        }

        throw IOException(e)
    }

}

inline fun <T> sdk29AndUp(onSdk29AndUp: () -> T): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        onSdk29AndUp()
    } else null
}

fun getMediaContentUri(type: Media.Type): Uri? {
    return when (type) {
        Media.Type.Image -> {
            sdk29AndUp {
                MediaStore.Images.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL_PRIMARY
                )
            } ?: MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        Media.Type.Audio -> {
            sdk29AndUp {
                MediaStore.Audio.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL_PRIMARY
                )
            } ?: MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        else -> {
            null
        }
    }
}