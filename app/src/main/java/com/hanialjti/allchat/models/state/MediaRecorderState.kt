package com.hanialjti.allchat.models.state

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.hanialjti.allchat.models.Attachment
import com.hanialjti.allchat.models.defaultAttachmentName
import com.hanialjti.allchat.models.entity.Media
import com.hanialjti.allchat.sdk26AndUp
import com.hanialjti.allchat.utils.createFileInInternalStorage
import com.hanialjti.allchat.utils.currentTimestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

@Stable
class MediaRecorderState(
    private var mediaRecorder: MediaRecorder? = null,
    private val coroutine: CoroutineScope,
    private val context: Context,
) {
    private var mediaOutputFile: File? = null

    private fun initialRecorder() {
        if (mediaRecorder == null) {
            mediaRecorder = sdk26AndUp { MediaRecorder(context) }
                ?: MediaRecorder()
        }
    }

    fun startRecording() {
        initialRecorder()
        coroutine.launch(Dispatchers.IO) {
            mediaOutputFile = context.createFileInInternalStorage(
                "AC_$currentTimestamp",
                Media.Type.Audio
            )
            mediaOutputFile?.createNewFile()

            mediaRecorder?.apply {

                reset()

                try {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                } catch (e: IllegalStateException) {
                    e.printStackTrace()
                }

                try {
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT)
                    setOutputFile(mediaOutputFile)
                    prepare()
                    start()
                } catch (e: IllegalStateException) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun stopMediaRecorder() {
        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    suspend fun stopRecording(): Attachment.Recording = withContext(Dispatchers.IO) {

        try {
            mediaRecorder?.stop()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        } catch (e: RuntimeException) {
            println("recording is too short")
            e.printStackTrace()
        }

        val copy = mediaOutputFile?.path?.let { File(it) }
            ?: throw IOException("Error while creating file")

        if (!copy.canRead())
            throw IOException("Error while reading file")


        val metadataRetriever = MediaMetadataRetriever()
        val recordingDuration: Int? = try {

            metadataRetriever.setDataSource(copy.path)
            val duration = metadataRetriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)

            duration?.toInt()
        } catch (e: RuntimeException) {
            e.printStackTrace()
            null
        } finally {
            metadataRetriever.release()
        }

        if ((recordingDuration ?: 0) <= 1000) {
            mediaOutputFile?.delete()
            mediaOutputFile = null
            println("deleting recording file")
            throw IOException("Recording is too short")
        }

        mediaOutputFile = null

        return@withContext Attachment.Recording(
            url = null,
            name = mediaOutputFile?.name ?: defaultAttachmentName,
            cacheUri = Uri.fromFile(copy).path,
            duration = recordingDuration ?: 0,
            size = 0,
        )
    }


    fun releaseRecorder() {
        try {
            mediaRecorder?.release()
            mediaRecorder = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}


@Composable
fun rememberMediaRecorderState(
    mediaRecorder: MediaRecorder? = null,
    coroutine: CoroutineScope = rememberCoroutineScope(),
    context: Context = LocalContext.current
): MediaRecorderState = remember {
    MediaRecorderState(
        mediaRecorder,
        coroutine,
        context
    )
}