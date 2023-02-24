package com.hanialjti.allchat.presentation.chat

import android.media.MediaPlayer
import androidx.compose.runtime.*
import com.hanialjti.allchat.common.utils.Logger
import com.hanialjti.allchat.data.model.Media
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Stable
class MediaPlayerState(
    private var mediaPlayer: MediaPlayer?,
    private val coroutine: CoroutineScope
) {

//    private var playing: String? = null

    var activeRecording: Media? by mutableStateOf(null)
        private set

    private fun initializePlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                setOnSeekCompleteListener {
                    it.start()
                }
            }
        }
    }

    fun pauseMedia(): Int {
        Logger.d { "Pausing media player..." }
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
        Logger.d { "Stopping media player..." }
        coroutine.launch {
            try {
                mediaPlayer?.stop()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun releasePlayer() {
        Logger.d { "Releasing media player..." }
        try {
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playMedia(recording: Media, seekTo: Int) {
        Logger.d { "Play media. RecordingUri: ${recording.cacheUri}, seekValue: $seekTo" }
        initializePlayer()

        try {

            mediaPlayer?.apply {

                if (activeRecording == null || recording != activeRecording) {
                    activeRecording = recording
                    reset()
                    setDataSource(recording.cacheUri)
                    prepare()
                }
                seekTo(seekTo)
                start()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

}

@Composable
fun rememberMediaPlayerState(
    mediaPlayer: MediaPlayer? = null,
    coroutine: CoroutineScope = rememberCoroutineScope()
): MediaPlayerState = remember {
    MediaPlayerState(
        mediaPlayer,
        coroutine
    )
}