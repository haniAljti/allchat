package com.hanialjti.allchat.component

import android.graphics.drawable.Icon
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.hanialjti.allchat.R
import com.hanialjti.allchat.models.UiMessage
import com.hanialjti.allchat.models.entity.Status
import com.hanialjti.allchat.ui.screens.MessageTime
import com.hanialjti.allchat.ui.screens.imageBottomCornerRadius
import com.hanialjti.allchat.ui.theme.Green
import com.hanialjti.allchat.ui.theme.SentMessageGradiant

@Composable
fun SentMessage(
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
        modifier = modifier
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MessageTime(
                timestamp = message.timestamp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            message.status?.let { messageStatus ->
                MessageStatusIcon(messageStatus = messageStatus)
            }
        }

        Column(
            modifier = Modifier
                .padding(end = 20.dp)
                .background(
                    brush = SentMessageGradiant,
                    shape = RoundedCornerShape(
                        topStart = 15.dp,
                        topEnd = if (lastMessageFromSameSender) 0.dp else 15.dp,
                        bottomStart = 15.dp
                    )
                )
                .weight(1f)
                .padding(1.dp)

        ) {

            message.attachment?.let {
                Attachment(
                    attachment = message.attachment,
                    modifier = modifier.clip(
                        RoundedCornerShape(
                            topEnd = if (lastMessageFromSameSender) 2.dp else 15.dp,
                            topStart = 15.dp,
                            bottomEnd = 3.dp,
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
    }
}

@Composable
fun MessageStatusIcon(messageStatus: Status) {
    when (messageStatus) {
        Status.Pending -> {
            MessageStatusIcon(
                iconRes = R.drawable.ic_pending,
                tint = MaterialTheme.colors.primary
            )
        }
        Status.Sent -> {
            MessageStatusIcon(
                iconRes = R.drawable.ic_sent,
                tint = MaterialTheme.colors.primary
            )
        }
        Status.Acknowledged -> {
            MessageStatusIcon(
                iconRes = R.drawable.ic_acknowledged,
                tint = MaterialTheme.colors.primary
            )
        }
        Status.Seen -> {
            MessageStatusIcon(
                iconRes = R.drawable.ic_acknowledged,
                tint = Green
            )
        }
        Status.Error -> {
            MessageStatusIcon(
                iconRes = R.drawable.ic_error,
                tint = Color.Red
            )
        }
    }
}

@Composable
fun MessageStatusIcon(
    modifier: Modifier = Modifier,
    iconRes: Int,
    tint: Color
) {
    Icon(
        painter = painterResource(id = iconRes),
        tint = tint,
        contentDescription = null
    )
}



@Composable
fun PlaceHolderMessage() {

    Box(
        modifier = Modifier
            .height(50.dp)
            .padding(end = 20.dp)
            .background(
                color = Color.Gray,
                shape = RoundedCornerShape(15.dp)
            )
            .padding(1.dp)

    )
}