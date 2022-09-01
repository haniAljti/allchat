package com.hanialjti.allchat

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class MediaRecorder(val context: Context) {
    private var mediaRecorder: MediaRecorder =
        (sdk26AndUp { MediaRecorder(context) } ?: MediaRecorder()).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        }

}

inline fun <T> sdk26AndUp(onSdk26AndUp: () -> T): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        onSdk26AndUp()
    } else null
}