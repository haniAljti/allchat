package com.hanialjti.allchat.data.remote.xmpp

import com.hanialjti.allchat.common.utils.Logger
import com.hanialjti.allchat.data.remote.FileUploader
import com.hanialjti.allchat.data.remote.model.CallResult
import com.hanialjti.allchat.data.remote.model.UploadProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smackx.httpfileupload.HttpFileUploadManager
import java.io.File

class XmppFileUploader(
    connection: XMPPTCPConnection,
    private val externalScope: CoroutineScope
): FileUploader {

    private val httpFileUploadManager = HttpFileUploadManager.getInstanceFor(connection)
    private val _filesUploadProgress = MutableStateFlow<MutableMap<Any, UploadProgress>>(
        mutableMapOf()
    )
    override val filesUploadProgress: SharedFlow<MutableMap<Any, UploadProgress>> get()  = _filesUploadProgress

    override suspend fun upload(file: File, identifier: Any): CallResult<String> {

        return try {
            var totalBytes: Long = 0

            val url = httpFileUploadManager.uploadFile(file) { uploaded, total ->

                totalBytes = total
                val uploadProgress = UploadProgress(
                    uploadedBytes = uploaded,
                    totalBytes = total,
                    isUploaded = false,
                    url = null
                )

                updateUploadProgress(identifier, uploadProgress)

            }
            Logger.d { "Uploading media progress: finished!" }

            val uploadProgress = UploadProgress(
                uploadedBytes = totalBytes,
                totalBytes = totalBytes,
                isUploaded = true,
                url = url.toString()
            )

            updateUploadProgress(identifier, uploadProgress)

            CallResult.Success(url.toString())
        } catch (e: Exception) {
            Logger.e(e)
            CallResult.Error("Could not upload file", e)
        }

    }

    private fun updateUploadProgress(identifier: Any, uploadProgress: UploadProgress) {
        externalScope.launch {
            _filesUploadProgress.emit(
                _filesUploadProgress.value.toMutableMap().apply {
                    this[identifier] = uploadProgress
                }
            )
        }
    }

}