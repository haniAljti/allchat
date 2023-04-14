package com.hanialjti.allchat.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.request.ImageRequest
import com.hanialjti.allchat.data.model.MessageItem
import com.hanialjti.allchat.data.remote.model.DownloadProgress

@Composable
fun ReceivedMessage(
    message: MessageItem.MessageData,
    downloadProgress: DownloadProgress?,
    nextMessage: MessageItem?,
    previousMessage: MessageItem?,
    onResumeAudio: (seekValue: Int) -> Unit,
    onPauseAudio: () -> Unit,
    onImageClicked: () -> Unit,
    onPdfClicked: () -> Unit,
    onReplyClicked: () -> Unit,
    modifier: Modifier = Modifier,
    isActiveMessage: Boolean,
    containerColor: Color,
    contentColor: Color
) {

    val isSameSenderFromNextMessage = remember(previousMessage, message) {
        derivedStateOf {
            previousMessage is MessageItem.MessageData && previousMessage.senderId == message.senderId
        }
    }

    Box(
        modifier = modifier
    ) {

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.padding(start = 20.dp)
        ) {

            if (!isSameSenderFromNextMessage.value) {
                message.senderImage?.AsImage(modifier = Modifier.size(30.dp))
            } else Spacer(modifier = Modifier.size(30.dp))

            BoxWithConstraints(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .background(
                        color = containerColor,
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.widthIn(100.dp, maxWidth * 0.85f)
                ) {

                    message.replyTo?.let {
                        ReplyingTo(
                            message = it, modifier = Modifier
                                .padding(horizontal = 10.dp)
                                .padding(top = 10.dp)
                                .align(Alignment.Start)
                        ) {
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
                            isActiveMessage = isActiveMessage,
                            containerColor = containerColor,
                            contentColor = contentColor
                        ) {
                            if (message.body.isNullOrEmpty()) {
                                Row(
                                    modifier = Modifier.padding(
                                        horizontal = 10.dp,
                                        vertical = 1.dp
                                    ),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = message.time,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp,
                                        modifier = Modifier.alpha(0.6f)
                                    )
                                }
                            }
                        }
                    }

                    if (!message.body.isNullOrEmpty()) {
                        Text(
                            text = message.body,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Start,
                            modifier = Modifier
                                .align(Alignment.Start)
                                .padding(horizontal = 10.dp)
                                .padding(top = 10.dp)
                        )
                    }

                    if (!message.body.isNullOrEmpty() || message.attachment?.type != com.hanialjti.allchat.data.model.Attachment.Type.Image) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 1.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = message.time,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .alpha(0.6f)
                                    .padding(horizontal = 5.dp)
                            )
                        }
                    }
                }
            }
        }
    }

}

//@Composable
//fun ReceivedMessage(
//    message: MessageItem.MessageData,
//    nextMessage: MessageItem?,
//    previousMessage: MessageItem?,
//    onResumeAudio: (seekValue: Int) -> Unit,
//    onPauseAudio: () -> Unit,
//    onImageClicked: () -> Unit,
//    onPdfClicked: () -> Unit,
//    modifier: Modifier = Modifier,
//    isActiveMessage: Boolean
//) {
//    Row(
//        verticalAlignment = Alignment.Bottom,
//        horizontalArrangement = Arrangement.Center,
//        modifier = modifier
//    ) {
//
//        val isNextMessageFromSameSender by remember {
//            mutableStateOf(
//                previousMessage != null && (previousMessage is MessageItem.MessageData && previousMessage.senderId == message.senderId || previousMessage !is MessageItem.MessageData)
//            )
//        }
//
//        Column(
//            modifier = Modifier
//                .padding(start = 20.dp)
//                .background(
//                    brush = ReceivedMessageGradiant,
//                    shape = RoundedCornerShape(
//                        topEnd = 15.dp,
//                        bottomStart = if (isNextMessageFromSameSender) 3.dp else 15.dp,
//                        bottomEnd = 15.dp
//                    )
//                )
//                .weight(1f)
//                .padding(1.dp),
////            horizontalAlignment = Alignment.Start
//
//        ) {
//            message.attachment?.let {
//                Attachment(
//                    attachment = it,
//                    downloadProgress = null,
//                    modifier = modifier.clip(
//                        RoundedCornerShape(
//                            topEnd = 15.dp,
//                            topStart = 3.dp,
//                            bottomEnd = imageBottomCornerRadius(message.body.isNullOrEmpty()),
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
//                    color = MaterialTheme.colors.primary,
//                    modifier = Modifier.padding(10.dp)
//                )
//            }
//
//        }
//
//        Column(horizontalAlignment = Alignment.CenterHorizontally) {
//            MessageTime(
//                timestamp = message.timestamp.toJavaLocalDateTime(),
//                modifier = Modifier.padding(horizontal = 20.dp)
//            )
////            message.status?.name?.let { Text(text = it, color = MaterialTheme.colors.primary) }
//        }
//
//    }
//
//}