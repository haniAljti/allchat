package com.hanialjti.allchat.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hanialjti.allchat.models.UiAttachment

@Composable
fun Attachment(
    attachment: UiAttachment,
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
        is UiAttachment.Image -> {
            val imageSource = attachment.url ?: attachment.cacheUri
            imageSource?.let {
                ImageAttachment(
                    image = attachment,
                    modifier = modifier,
                    onImageClicked = { onImageClicked() }
                )
            }
        }
        is UiAttachment.Recording -> AudioAttachment(
            recording = attachment,
            onResumeAudio = onResumeAudio,
            onPauseAudio = onPauseAudio,
            onSeekValueChanged = onAudioSeekValueChanged,
            isActiveMessage = isActiveMessage,
            lastTrackPosition = lastTrackPosition
        )
        is UiAttachment.Pdf -> PdfFileAttachment(
            pdf = attachment,
            onPdfClicked = onPdfClicked,
            modifier = Modifier
        )
        else -> {}
    }
}