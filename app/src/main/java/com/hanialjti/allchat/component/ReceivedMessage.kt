package com.hanialjti.allchat.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hanialjti.allchat.models.UiMessage
import com.hanialjti.allchat.ui.screens.MessageTime
import com.hanialjti.allchat.ui.screens.imageBottomCornerRadius
import com.hanialjti.allchat.ui.theme.ReceivedMessageGradiant

@Composable
fun ReceivedMessage(
    message: UiMessage,
    lastMessageFromSameSender: Boolean,
    onResumeAudio: () -> Unit,
    onPauseAudio: () -> Unit,
    onAudioSeekValueChanged: (Int) -> Unit,
    onImageClicked: () -> Unit,
    onPdfClicked: () -> Unit,
    modifier: Modifier = Modifier,
    isActiveMessage: Boolean,
    lastTrackPosition: Int
) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
    ) {

        Column(
            modifier = Modifier
                .padding(start = 20.dp)
                .background(
                    brush = ReceivedMessageGradiant,
                    shape = RoundedCornerShape(
                        topEnd = 15.dp,
                        bottomStart = if (lastMessageFromSameSender) 3.dp else 15.dp,
                        bottomEnd = 15.dp
                    )
                )
                .weight(1f)
                .padding(1.dp),
//            horizontalAlignment = Alignment.Start

        ) {
            message.attachment?.let {
                Attachment(
                    attachment = message.attachment,
                    modifier = modifier.clip(
                        RoundedCornerShape(
                            topEnd = 15.dp,
                            topStart = 3.dp,
                            bottomEnd = imageBottomCornerRadius(message.body.isNullOrEmpty()),
                            bottomStart = imageBottomCornerRadius(message.body.isNullOrEmpty())
                        )
                    ),
                    onResumeAudio = onResumeAudio,
                    onPauseAudio = onPauseAudio,
                    onAudioSeekValueChanged = onAudioSeekValueChanged,
                    onImageClicked = onImageClicked,
                    onPdfClicked = onPdfClicked,
                    isActiveMessage = isActiveMessage,
                    lastTrackPosition = lastTrackPosition
                )
            }

            if (!message.body.isNullOrEmpty()) {
                Text(
                    text = message.body,
                    color = Color.White,
                    modifier = Modifier.padding(10.dp)
                )
            }

        }
        MessageTime(
            timestamp = message.timestamp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
    }

}