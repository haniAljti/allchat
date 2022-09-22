package com.hanialjti.allchat.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.tooling.preview.Preview
import com.hanialjti.allchat.component.ReceivedMessage
import com.hanialjti.allchat.models.UiMessage
import com.hanialjti.allchat.models.entity.Message
import java.util.*

val SentMessageGradiant = Brush.linearGradient(
    listOf(Color(0xFF26DBB5), Color(0xFF71BCAC)),
    Offset.Infinite,
    Offset.Infinite
)

val ReceivedMessageGradiant = Brush.radialGradient(
    colors = listOf(Color(0x1A26DBB5), Color(0x4DFFFFFF)),
    center = Offset(1000f, -100f),
    radius = 1500f
)

@Preview
@Composable
fun PreviewReceivedMessageGradiant() {
    ReceivedMessage(
        message = UiMessage(
            body = "Hi\nksdkfm",
            id = 1
        ),
        lastMessageFromSameSender = false,
        onResumeAudio = {  },
        onPauseAudio = {  },
        onAudioSeekValueChanged = {  },
        onImageClicked = {  },
        onPdfClicked = {  },
        isActiveMessage = false,
        lastTrackPosition = 0
    )
}