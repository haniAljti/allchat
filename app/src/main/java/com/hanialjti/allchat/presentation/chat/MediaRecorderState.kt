package com.hanialjti.allchat.presentation.chat

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.hanialjti.allchat.common.utils.sdkEqualsOrUp
import com.hanialjti.allchat.data.model.Attachment
import com.hanialjti.allchat.data.model.Media
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException

@Stable
class MediaRecorderState(
    private var mediaRecorder: MediaRecorder? = null,
    private val coroutine: CoroutineScope,
    private val context: Context,
) {
    private var mediaOutputFile: File? = null
    private val _mediaRecorderCurrentState = MutableStateFlow(State.Initial)
    var mediaRecorderCurrentState: StateFlow<State> = _mediaRecorderCurrentState

    enum class State { Initial, Initialized, DataSourceConfigured, Prepared, Recording, Released }

    @SuppressLint("NewApi")
    private fun initializeRecorder() {
        if (mediaRecorder == null) {
            mediaRecorder = sdkEqualsOrUp(Build.VERSION.SDK_INT) { MediaRecorder(context) }
                ?: MediaRecorder()
        }
    }

    fun startRecording(file: File) {
        initializeRecorder()
        Timber.d("Recording audio...")
        coroutine.launch(Dispatchers.IO) {
//            mediaOutputFile = context.createFileInInternalStorage(
//                "AC_$currentTimestamp",
//                UiAttachment.Type.Audio
//            )
//            mediaOutputFile?.createNewFile()
            mediaOutputFile = file

            mediaRecorder?.apply {

                reset()
                Timber.d("Media Recorde is reset")
                _mediaRecorderCurrentState.update { State.Initial }

                try {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    Timber.d("Set Audio source: success")
                    _mediaRecorderCurrentState.update { State.Initialized }
                } catch (e: IllegalStateException) {
                    Timber.e("Failed to set audio source")
                    e.printStackTrace()
                }

                try {
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    Timber.d("Set output format: success")
                    setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC)
                    Timber.d("Set audio encoder: success")
                    setOutputFile(mediaOutputFile)
                    Timber.d("Set output file: success")
                    _mediaRecorderCurrentState.update { State.DataSourceConfigured }
                    prepare()
                    Timber.d("Prepare: success")
                    _mediaRecorderCurrentState.update { State.Prepared }
                    start()
                    Timber.d("Start: success")
                    _mediaRecorderCurrentState.update { State.Recording }
                } catch (e: IllegalStateException) {
                    Timber.d("Illegal state -> startRecording()")
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun cancelRecording() {
        Timber.d("cancelling recording")
        try {
            mediaRecorder?.stop()
            _mediaRecorderCurrentState.update { State.Initial }
            mediaOutputFile?.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopMediaRecorder() {
        try {
            mediaRecorder?.stop()
            _mediaRecorderCurrentState.update { State.Initial }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    suspend fun stopRecording(): Attachment = withContext(Dispatchers.IO) {

        try {
            mediaRecorder?.stop()
            _mediaRecorderCurrentState.update { State.Initial }
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        } catch (e: RuntimeException) {
            println("recording is too short")
            e.printStackTrace()
        }

        val path = mediaOutputFile?.path

        val metadataRetriever = MediaMetadataRetriever()
        val recordingDuration: Int? = try {
            metadataRetriever.setDataSource(path)
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

        return@withContext Media(
            cacheUri = path,
            duration = recordingDuration?.toLong() ?: 0,
            type = Attachment.Type.Audio,
        )
    }


    fun releaseRecorder() {
        try {
            mediaRecorder?.release()
            _mediaRecorderCurrentState.update { State.Released }
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