package com.hanialjti.allchat.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanialjti.allchat.R
import com.hanialjti.allchat.data.model.Attachment
import com.hanialjti.allchat.data.model.MessageItem
import com.hanialjti.allchat.data.model.MessageStatus
import com.hanialjti.allchat.data.model.ReplyingToMessage
import com.hanialjti.allchat.data.remote.model.DownloadProgress
import com.hanialjti.allchat.data.remote.model.UploadProgress

//@Composable
//fun PCSentMessage(
//    message: MessageItem.MessageData,
//    nextMessage: MessageItem?,
//    previousMessage: MessageItem?,
//    onResumeAudio: (Int) -> Unit,
//    onPauseAudio: () -> Unit,
//    onAudioSeekValueChanged: (Int) -> Unit,
//    onImageClicked: () -> Unit,
//    onPdfClicked: () -> Unit,
//    modifier: Modifier = Modifier,
//    isActiveMessage: Boolean,
//    lastTrackPosition: Int
//) {
//    Row(
//        verticalAlignment = Alignment.Bottom,
//        modifier = modifier
//    ) {
//        Column(horizontalAlignment = Alignment.CenterHorizontally) {
//            MessageTime(
//                timestamp = message.timestamp.toJavaLocalDateTime(),
//                modifier = Modifier.padding(horizontal = 20.dp)
//            )
//            MessageStatusIcon(messageStatus = message.status)
//        }
//
//        val isNextMessageFromSameSender by remember {
//            mutableStateOf(
//                nextMessage == null || nextMessage is MessageItem.MessageData && nextMessage.senderId == message.senderId
//            )
//        }
//
//        Column(
//            modifier = Modifier
//                .padding(end = 20.dp)
//                .background(
//                    brush = SentMessageGradiant,
//                    shape = RoundedCornerShape(
//                        topStart = 15.dp,
//                        topEnd = if (isNextMessageFromSameSender) 0.dp else 15.dp,
//                        bottomStart = 15.dp
//                    )
//                )
//                .weight(1f)
//                .padding(1.dp)
//
//        ) {
//
//            message.attachment?.uiAttachment?.let {
//                Attachment(
//                    attachment = it,
//                    modifier = modifier.clip(
//                        RoundedCornerShape(
//                            topEnd = if (isNextMessageFromSameSender) 2.dp else 15.dp,
//                            topStart = 15.dp,
//                            bottomEnd = 3.dp,
//                            bottomStart = imageBottomCornerRadius(message.body.isNullOrEmpty())
//                        )
//                    ),
//                    onResumeAudio = onResumeAudio,
//                    onPauseAudio = onPauseAudio,
//                    onImageClicked = onImageClicked,
//                    onPdfClicked = onPdfClicked,
//                    isActiveMessage = isActiveMessage
//                )
//            }
//
//            if (!message.body.isNullOrEmpty()) {
//                Text(
//                    text = message.body,
//                    color = Color.White,
//                    modifier = Modifier.padding(10.dp)
//                )
//            }
//        }
//    }
//}

@Composable
fun SentMessage(
    message: MessageItem.MessageData,
    uploadProgress: UploadProgress?,
    downloadProgress: DownloadProgress?,
    nextMessage: MessageItem?,
    previousMessage: MessageItem?,
    onResumeAudio: (Int) -> Unit,
    onPauseAudio: () -> Unit,
    onImageClicked: () -> Unit,
    onPdfClicked: () -> Unit,
    modifier: Modifier = Modifier,
    onReplyClicked: () -> Unit,
    isActiveMessage: Boolean
) {
    Box(
        modifier = modifier
    ) {

        Column(
            modifier = Modifier
                .widthIn(100.dp, 350.dp)
                .width(IntrinsicSize.Max)
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp)
                .background(
                    color = Color(0xFF3E5A55),
                    shape = RoundedCornerShape(20.dp)
                ),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Bottom
        ) {

            message.replyTo?.let {
                ReplyingTo(message = it, modifier = Modifier
                    .padding(horizontal = 10.dp).padding(top = 10.dp)
                    .align(Alignment.Start)) {
                    onReplyClicked()
                }
            }

            message.attachment?.let {
                Attachment(
                    attachment = it,
                    downloadProgress = downloadProgress,
                    modifier = modifier.clip(RoundedCornerShape(20.dp)),
                    onResumeAudio = onResumeAudio,
                    onPauseAudio = onPauseAudio,
                    onImageClicked = onImageClicked,
                    onPdfClicked = onPdfClicked,
                    isActiveMessage = isActiveMessage
                ) {
                    if (message.body.isNullOrEmpty()) {
                        StatusAndTime(message = message, uploadProgress = uploadProgress)
                    }
                }
            }

            if (!message.body.isNullOrEmpty()) {
                Text(
                    text = message.body,
                    color = Color.White,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(horizontal = 10.dp)
                        .padding(top = 10.dp)
                )
            }

            if (!message.body.isNullOrEmpty() || message.attachment?.type != Attachment.Type.Image) {
                StatusAndTime(message = message, uploadProgress = uploadProgress)
            }

        }
    }
}

@Composable
fun ReplyingTo(message: ReplyingToMessage, modifier: Modifier = Modifier, onClicked: () -> Unit) {
    Row(modifier = modifier.fillMaxWidth().clickable { onClicked() }) {
        Spacer(
            modifier = Modifier
                .padding(end = 3.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Green)
                .width(5.dp)
                .height(55.dp)
        )
        Column {
            message.senderName?.let {
                Text(text = it)
            }
            message.body?.let {
                Text(text = it, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } ?: message.attachment?.let {
                Text(text = it.type.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun StatusAndTime(message: MessageItem.MessageData, uploadProgress: UploadProgress?) {
    Row(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 1.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message.time,
            color = MaterialTheme.colors.primary,
            fontSize = 12.sp,
            modifier = Modifier.alpha(0.6f)
        )
        Spacer(modifier = Modifier.width(3.dp))
        if (uploadProgress != null && !uploadProgress.isUploaded) {
            CircularProgressIndicator(
                progress = (uploadProgress.uploadedBytes.toFloat() / uploadProgress.totalBytes),
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(20.dp),
                strokeWidth = 2.dp
            )
        } else {
            MessageStatusIcon(
                messageStatus = message.status,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun MessageStatusIcon(messageStatus: MessageStatus, modifier: Modifier = Modifier) {
    when (messageStatus) {
        MessageStatus.Pending -> {
            MessageStatusIcon(
                iconRes = R.drawable.ic_pending,
                tint = MaterialTheme.colors.primary,
                modifier = modifier
            )
        }
        MessageStatus.Sent -> {
            MessageStatusIcon(
                iconRes = R.drawable.ic_sent,
                tint = MaterialTheme.colors.primary,
                modifier = modifier
            )
        }
        MessageStatus.Delivered -> {
            MessageStatusIcon(
                iconRes = R.drawable.ic_acknowledged,
                tint = MaterialTheme.colors.primary,
                modifier = modifier
            )
        }
        MessageStatus.Seen -> {
            MessageStatusIcon(
                iconRes = R.drawable.ic_acknowledged,
                tint = Color(0xFF26DBB5),
                modifier = modifier
            )
        }
        MessageStatus.Error -> {
            MessageStatusIcon(
                iconRes = R.drawable.ic_error,
                tint = Color.Red,
                modifier = modifier
            )
        }
        else -> {}
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