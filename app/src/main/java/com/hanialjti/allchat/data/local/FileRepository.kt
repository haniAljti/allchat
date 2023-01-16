package com.hanialjti.allchat.data.local

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okio.use
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL

class FileRepository(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher
) {

    suspend fun writeToInternalStorage(bytes: ByteArray, fileName: String): Uri =
        withContext(dispatcher) {
            return@withContext try {

                val imageFile = File(context.cacheDir, fileName)
                val fileOutputStream = FileOutputStream(imageFile)
                fileOutputStream.use { stream ->
                    try {
                        stream.write(bytes)
                    } catch (e: IOException) {
                        throw IOException("Couldn't save bitmap.")
                    }
                }
                Uri.fromFile(imageFile)
            } catch (e: IOException) {
                e.printStackTrace()
                throw IOException("Error while creating image")
            }
        }

    suspend fun writeToInternalStorage(url: String, fileName: String): Uri =
        withContext(dispatcher) {
            return@withContext try {

                val imageFile = File(context.cacheDir, fileName)
                val fileOutputStream = FileOutputStream(imageFile)
                URL(url)
                    .openConnection()
                    .getInputStream()
                    .use { inputStream ->
                        fileOutputStream.use { fileOutputStream ->
                            inputStream.copyTo(fileOutputStream)
                        }
                    }

                Uri.fromFile(imageFile)
            } catch (e: IOException) {
                e.printStackTrace()
                throw IOException("Error while creating image")
            }
        }
}