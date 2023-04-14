package com.hanialjti.allchat.presentation.component

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.request.ImageRequest
import com.hanialjti.allchat.data.model.Attachment
import com.hanialjti.allchat.data.model.Media
import com.hanialjti.allchat.data.remote.model.DownloadProgress

@Composable
fun Attachment(
    attachment: Attachment,
    downloadProgress: DownloadProgress?,
    modifier: Modifier = Modifier,
    onResumeAudio: (Int) -> Unit,
    onPauseAudio: () -> Unit,
    onImageClicked: () -> Unit,
    onPdfClicked: () -> Unit,
    isActiveMessage: Boolean,
    contentColor: Color,
    containerColor: Color,
    timestampAndStatus: @Composable () -> Unit = { }
) {
    when (attachment.type) {
        Attachment.Type.Image -> {
            ImageAttachment(
                image = attachment as Media,
                modifier = modifier,
                onImageClicked = onImageClicked,
                timestampAndStatus = timestampAndStatus,
                containerColor = containerColor,
                contentColor = contentColor
            )
        }
        Attachment.Type.Audio -> AudioAttachment(
            recording = attachment as Media,
            downloadProgress = downloadProgress,
            onResumeAudio = onResumeAudio,
            onPauseAudio = onPauseAudio,
            isActiveMessage = isActiveMessage,
            modifier = modifier
                .padding(horizontal = 10.dp)
                .padding(top = 10.dp),
            containerColor = containerColor,
            contentColor = contentColor
        )
        Attachment.Type.Document -> PdfFileAttachment(
            pdf = attachment as Media,
            downloadProgress = downloadProgress,
            onPdfClicked = onPdfClicked,
            modifier = modifier
        )
        else -> {}
    }
}