package com.hanialjti.allchat.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.hanialjti.allchat.R
import com.hanialjti.allchat.data.model.MessageItem
import com.hanialjti.allchat.data.model.MessageStatus
import com.hanialjti.allchat.presentation.chat.MessageTime
import com.hanialjti.allchat.presentation.chat.imageBottomCornerRadius
import com.hanialjti.allchat.presentation.ui.theme.Green50
import com.hanialjti.allchat.presentation.ui.theme.SentMessageGradiant

@Composable
fun SentMessage(
    message: MessageItem.MessageData,
    nextMessage: MessageItem?,
    previousMessage: MessageItem?,
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
            MessageStatusIcon(messageStatus = message.status)
        }

        val isNextMessageFromSameSender by remember {
            mutableStateOf(
                nextMessage == null || nextMessage is MessageItem.MessageData && nextMessage.senderId == message.senderId
            )
        }

        Column(
            modifier = Modifier
                .padding(end = 20.dp)
                .background(
                    brush = SentMessageGradiant,
                    shape = RoundedCornerShape(
                        topStart = 15.dp,
                        topEnd = if (isNextMessageFromSameSender) 0.dp else 15.dp,
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
                            topEnd = if (isNextMessageFromSameSender) 2.dp else 15.dp,
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
fun MessageStatusIcon(messageStatus: MessageStatus) {
    when (messageStatus) {
        MessageStatus.Pending -> {
            MessageStatusIcon(
                iconRes = R.drawable.ic_pending,
                tint = MaterialTheme.colors.primary
            )
        }
        MessageStatus.Sent -> {
            MessageStatusIcon(
                iconRes = R.drawable.ic_sent,
                tint = MaterialTheme.colors.primary
            )
        }
        MessageStatus.Delivered -> {
            MessageStatusIcon(
                iconRes = R.drawable.ic_acknowledged,
                tint = MaterialTheme.colors.primary
            )
        }
        MessageStatus.Seen -> {
            MessageStatusIcon(
                iconRes = R.drawable.ic_acknowledged,
                tint = Color(0xFF26DBB5)
            )
        }
        MessageStatus.Error -> {
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
        modifier = modifier,
        painter = painterResource(id = iconRes),
        tint = tint,
        contentDescription = null
    )
}



@Composable
fun PlaceHolderMessage() {

    Box(
        modifier = Modifier
            .padding(end = 20.dp)
            .height(40.dp)
            .fillMaxWidth()
            .background(
                color = Color.Gray,
                shape = RoundedCornerShape(15.dp)
            )
            .padding(1.dp)

    )
}