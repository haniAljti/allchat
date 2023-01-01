package com.hanialjti.allchat.presentation.component

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FloatTweenSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import com.hanialjti.allchat.R
import com.hanialjti.allchat.presentation.chat.Attachment
import com.hanialjti.allchat.presentation.chat.AudioPlayBackButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import rm.com.audiowave.AudioWaveView
import rm.com.audiowave.OnProgressListener
import java.io.File

@Composable
fun AudioAttachment(
    recording: Attachment.Recording,
    onSeekValueChanged: (Int) -> Unit,
    onResumeAudio: () -> Unit,
    onPauseAudio: () -> Unit,
    isActiveMessage: Boolean,           // Helps to stop other message from playing
    lastTrackPosition: Int              // Last position where this audio stopped
) {

    var isPlaying by remember { mutableStateOf(false) }

    val animatedValue = remember { Animatable(0f) }
    val coroutine = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutine.launch { animatedValue.snapTo((lastTrackPosition * 100 / (recording.duration?.toFloat() ?: 1f))) }
    }

    LaunchedEffect(isPlaying, isActiveMessage, animatedValue) {

        if (isPlaying && isActiveMessage) {

            animatedValue.animateTo(
                targetValue = 100f,
                animationSpec = FloatTweenSpec(
                    (recording.duration ?: 1).toInt() - (((recording.duration ?: 1) * animatedValue.value).toInt() / 100),
                    0,
                    LinearEasing
                )
            )

        } else {
            animatedValue.stop()
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {

        AndroidView(
            modifier = Modifier
                .height(50.dp)
                .padding(start = 10.dp)
                .weight(1f),
            factory = { context ->
                AudioWaveView(context).apply {
                    this.minChunkHeight = 10
                    this.chunkHeight = 80
                    this.waveColor =
                        ResourcesCompat.getColor(resources, R.color.white, context.theme)
                    this.chunkWidth = 5
                    this.chunkRadius = 5
                    this.chunkSpacing = 10

                    coroutine.launch(Dispatchers.IO) {
                        recording.cacheUri?.let {
                            Uri.parse(it).path?.let { mediaFilePath ->
                                val file = File(mediaFilePath)
                                if (file.exists()) {
                                    setRawData(file.readBytes())
                                }
                            }
                        }
                    }



                    this.onProgressListener = object : OnProgressListener {
                        override fun onProgressChanged(progress: Float, byUser: Boolean) {
                            if (progress == 100f) {
                                isPlaying = false
                            }
                        }

                        override fun onStartTracking(progress: Float) {
                            coroutine.launch { animatedValue.stop() }
                            onPauseAudio()
                            isPlaying = false
                        }

                        override fun onStopTracking(progress: Float) {
                            coroutine.launch {
                                animatedValue.snapTo(progress)
                                onSeekValueChanged(
                                    (((recording.duration ?: 1) * animatedValue.value).toInt() / 100)
                                )
                            }
                        }
                    }
                }
            },
            update = { it.progress = animatedValue.value }
        )

        if (isPlaying && isActiveMessage) {
            AudioPlayBackButton(imageRes = R.drawable.ic_pause) {
                onPauseAudio()
                isPlaying = false
            }
        } else {
            AudioPlayBackButton(imageRes = R.drawable.ic_play) {
                coroutine.launch {
                    if (animatedValue.value == 100f) {
                        animatedValue.snapTo(0f)
                        isPlaying = false
                    }
                    onResumeAudio()
                    isPlaying = true
                }

            }
        }
    }

}