package com.hanialjti.allchat.presentation.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.hanialjti.allchat.data.model.MessageItem
import com.hanialjti.allchat.presentation.component.ReceivedMessage

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
        message = MessageItem.MessageData(
            body = "Hi\nksdkfm",
            id = "1",
            attachment = null,
            senderId = "2",
            senderImage = null,
            senderName = null
        ),
        nextMessage = MessageItem.MessageData(
            body = "Hi\nksdkfm",
            id = "2",
            attachment = null,
            senderId = "2",
            senderImage = null,
            senderName = null
        ),
        previousMessage = MessageItem.MessageData(
            body = "Hi\nksdkfm",
            id = "3",
            attachment = null,
            senderId = "2",
            senderImage = null,
            senderName = null
        ),
        onResumeAudio = {  },
        onPauseAudio = {  },
        onAudioSeekValueChanged = {  },
        onImageClicked = {  },
        onPdfClicked = {  },
        isActiveMessage = false,
        lastTrackPosition = 0
    )
}