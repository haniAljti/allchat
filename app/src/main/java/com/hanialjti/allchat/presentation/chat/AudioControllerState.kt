package com.hanialjti.allchat.presentation.chat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FloatTweenSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import timber.log.Timber

class AudioControllerState(
    val initialValue: Int,
    var audioDuration: Int
) {

    private val animatable = Animatable(initialValue.toFloat())

    val value get() = animatable.value

    val isPlaying
        get() = animatable.isRunning

    val durationString get() = mutableStateOf(convertSecondToHHMMSSString(audioDuration))

    private fun convertSecondToHHMMSSString(millis: Int): String {
        val timeInSeconds = millis.div(1000)
        val seconds = timeInSeconds.mod(60)
        val minutes = timeInSeconds.mod(3600).div(60)
        return String.format("%02d:%02d", minutes, seconds)
    }

    suspend fun play() {
        if (animatable.value.toInt() == audioDuration)
            snapTo(0f)
        animatable.animateTo(
            targetValue = audioDuration.toFloat(),
            animationSpec = FloatTweenSpec(
                 audioDuration - animatable.value.toInt(),
                0,
                LinearEasing
            )
        )
    }

    suspend fun stop() {
        animatable.stop()
    }

    suspend fun snapTo(value: Float) {
        animatable.snapTo(value)
    }

    companion object {
        fun Saver(
            audioDuration: Int
        ) = Saver<AudioControllerState, Int>(
            save = { it.value.toInt() },
            restore = { AudioControllerState(it, audioDuration) }
        )
    }
}

@Composable
fun rememberAudioControllerState(
    initialValue: Int,
    audioDuration: Int
): AudioControllerState {
    return rememberSaveable(
        saver = AudioControllerState.Saver(audioDuration)
    ) {
        AudioControllerState(
            initialValue = initialValue,
            audioDuration = audioDuration
        )
    }
}