package com.hanialjti.allchat.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hanialjti.allchat.models.Attachment

@Composable
fun Attachment(
    attachment: Attachment,
    modifier: Modifier = Modifier,
    onResumeAudio: () -> Unit,
    onPauseAudio: () -> Unit,
    onAudioSeekValueChanged: (Int) -> Unit,
    onImageClicked: () -> Unit,
    onPdfClicked: () -> Unit,
    isActiveMessage: Boolean,
    lastTrackPosition: Int
) {
    when (attachment) {
        is Attachment.Image -> {
            val imageSource = attachment.url ?: attachment.cacheUri
            imageSource?.let {
                ImageAttachment(
                    image = attachment,
                    modifier = modifier,
                    onImageClicked = { onImageClicked() }
                )
            }
        }
        is Attachment.Recording -> AudioAttachment(
            recording = attachment,
            onResumeAudio = onResumeAudio,
            onPauseAudio = onPauseAudio,
            onSeekValueChanged = onAudioSeekValueChanged,
            isActiveMessage = isActiveMessage,
            lastTrackPosition = lastTrackPosition
        )
        is Attachment.Pdf -> PdfFileAttachment(
            pdf = attachment,
            onPdfClicked = onPdfClicked,
            modifier = Modifier
        )
        else -> {}
    }
}