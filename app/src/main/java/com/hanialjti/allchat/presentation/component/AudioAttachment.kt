package com.hanialjti.allchat.presentation.component

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.hanialjti.allchat.R
import com.hanialjti.allchat.data.model.Media
import com.hanialjti.allchat.data.remote.model.DownloadProgress
import com.hanialjti.allchat.presentation.chat.AudioPlayBackButton
import com.hanialjti.allchat.presentation.chat.rememberAudioControllerState
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AudioAttachment(
    recording: Media,
    downloadProgress: DownloadProgress?,
    onResumeAudio: (Int) -> Unit,
    onPauseAudio: () -> Unit,
    isActiveMessage: Boolean,           // Helps to stop other recordings from playing
    modifier: Modifier = Modifier
) {

    val audioControllerState = rememberAudioControllerState(
        initialValue = 0,
        audioDuration = recording.duration?.toInt() ?: 0
    )

    LaunchedEffect(recording) {
        audioControllerState.audioDuration = recording.duration?.toInt() ?: 0
    }

    var sliderValue by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()
    val durationString = audioControllerState.durationString.value
    val recordingStatus by remember(
        audioControllerState.isPlaying,
        isActiveMessage,
        recording,
        downloadProgress
    ) {
        derivedStateOf {
            when {
                recording.cacheUri == null -> RecordingStatus.NotDownloaded
                downloadProgress != null && !downloadProgress.isFullyDownloaded -> RecordingStatus.Downloading
                audioControllerState.isPlaying && isActiveMessage -> RecordingStatus.Playing
                else -> RecordingStatus.Ready
            }
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.width(350.dp)) {

        AnimatedContent(
            targetState = recordingStatus,
            transitionSpec = { scaleIn() with scaleOut() }
        ) {
            val buttonModifier = Modifier
                .background(color = Color(0x4D191D18), shape = RoundedCornerShape(15.dp))
                .size(45.dp)
            when (it) {
                RecordingStatus.NotDownloaded -> {
                    Box(modifier = buttonModifier.clickable { onResumeAudio(0) }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_down),
                            contentDescription = null,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
                RecordingStatus.Downloading -> {
                    Box(modifier = buttonModifier) {
                        if (downloadProgress != null) {
                            CircularProgressIndicator(
                                progress = (downloadProgress.downloadedBytes.toFloat() / downloadProgress.totalBytes),
                                modifier = Modifier.padding(10.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
                RecordingStatus.Ready -> {
                    AudioPlayBackButton(
                        imageRes = R.drawable.ic_play,
                        modifier = buttonModifier
                    ) {
                        onResumeAudio(if (recording.duration?.toInt() == audioControllerState.value.toInt()) 0 else audioControllerState.value.toInt())
                        scope.launch { audioControllerState.play() }
                    }
                }
                RecordingStatus.Playing -> {
                    AudioPlayBackButton(
                        imageRes = R.drawable.ic_pause,
                        modifier = buttonModifier
                    ) {
                        onPauseAudio()
                        scope.launch { audioControllerState.stop() }
                    }
                }
            }

        }

        Slider(
            value = audioControllerState.value,
            onValueChange = {
                onPauseAudio()
                scope.launch {
                    audioControllerState.stop()
                    audioControllerState.snapTo(it)
                }
                sliderValue = it
            },
            colors = SliderDefaults.colors(
                activeTrackColor = Color(0xFF191D18)
            ),
            modifier = Modifier.weight(1f),
            valueRange = 0f..(recording.duration?.toFloat() ?: 0f),
            thumb = {
                Box(
                    modifier = Modifier
                        .padding(15.dp)
                        .background(Color.White, shape = RoundedCornerShape(50))
                        .width(5.dp)
                        .height(10.dp)
                )
            }
        )

        Text(text = durationString, color = Color.White)


//        AndroidView(
//            modifier = Modifier
//                .height(50.dp)
//                .weight(1f),
//            factory = { context ->
//                AudioWaveView(context).apply {
//                    this.minChunkHeight = 10
//                    this.chunkHeight = 80
//                    this.waveColor =
//                        ResourcesCompat.getColor(resources, R.color.white, context.theme)
//                    this.chunkWidth = 5
//                    this.chunkRadius = 5
//                    this.chunkSpacing = 10
//
//                    coroutine.launch(Dispatchers.IO) {
//                        recording.cacheUri?.let {
//                            Uri.parse(it).path?.let { mediaFilePath ->
//                                val file = File(mediaFilePath)
//                                if (file.exists()) {
//                                    setRawData(file.readBytes())
//                                }
//                            }
//                        }
//                    }
//
//                    this.onProgressListener = object : OnProgressListener {
//                        override fun onProgressChanged(progress: Float, byUser: Boolean) {
//                            if (progress == 100f) {
//                                isPlaying = false
//                            }
//                        }
//
//                        override fun onStartTracking(progress: Float) {
//                            coroutine.launch { animatedValue.stop() }
//                            onPauseAudio()
//                            isPlaying = false
//                        }
//
//                        override fun onStopTracking(progress: Float) {
//                            coroutine.launch {
//                                animatedValue.snapTo(progress)
//                                onSeekValueChanged(
//                                    (((recording.duration
//                                        ?: 1) * animatedValue.value).toInt() / 100)
//                                )
//                            }
//                        }
//                    }
//                }
//            },
//            update = { it.progress = animatedValue.value }
//        )
    }

}

private enum class RecordingStatus { NotDownloaded, Downloading, Ready, Playing }