package com.hanialjti.allchat.models.state

import android.media.MediaPlayer
import android.util.Log
import androidx.compose.runtime.*
import com.hanialjti.allchat.models.UiAttachment
import com.hanialjti.allchat.models.UiMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Stable
class MediaPlayerState(
    private var mediaPlayer: MediaPlayer?,
    private val coroutine: CoroutineScope,
    val activeRecording: MutableState<UiAttachment.Recording?>,
) {

    init {
        mediaPlayer?.apply {
            setOnSeekCompleteListener {
                Log.d("OnSeekCompleteListener", "called")
                it.start()
            }
        }
    }

    fun pauseMedia(): Int {
        coroutine.launch {
            try {
                mediaPlayer?.pause()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return mediaPlayer?.currentPosition ?: 0
    }

    fun stopMedia() {
        coroutine.launch {
            try {
                mediaPlayer?.stop()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun releasePlayer() {
        try {
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playMedia(recording: UiAttachment.Recording, trackPosition: Int?) {
        coroutine.launch(Dispatchers.IO) {
            try {

                mediaPlayer?.apply {

                    reset()
                    setDataSource(recording.cacheUri)
                    prepare()

                    if (trackPosition != null) {
                        seekTo(trackPosition)
                    } else {
                        start()
                    }

                }
                activeRecording.value = recording

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}

@Composable
fun rememberMediaPlayerState(
    mediaPlayer: MediaPlayer?,
    coroutine: CoroutineScope = rememberCoroutineScope()
): MediaPlayerState = remember {
    MediaPlayerState(
        mediaPlayer,
        coroutine,
        mutableStateOf(null)
    )
}