package com.hanialjti.allchat.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.hanialjti.allchat.ChatViewModel
import com.hanialjti.allchat.R
import com.hanialjti.allchat.models.Media
import com.hanialjti.allchat.models.Message
import com.hanialjti.allchat.ui.theme.Gray
import com.hanialjti.allchat.ui.theme.Green
import com.hanialjti.allchat.ui.theme.Orange
import com.hanialjti.allchat.utils.TWO_DIGIT_FORMAT
import com.hanialjti.allchat.utils.formatDateSeparator
import com.hanialjti.allchat.utils.formatTimestamp
import rm.com.audiowave.AudioWaveView
import rm.com.audiowave.OnProgressListener

@Composable
fun ChatScreen(
    navController: NavHostController,
    viewModel: ChatViewModel = hiltViewModel()
) {
    Box {

        Column {
            ChatTopBar(
                onBackClicked = { navController.popBackStack() },
                onPersonClicked = { /*TODO*/ },
                onMenuClicked = { /*TODO*/ }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                MessagesList(
                    messages = viewModel.getDummyMessageList(),
                    modifier = Modifier.align(Alignment.BottomEnd),
                    onAudioStatusChanged = { message, audioStatus -> /*TODO*/ },
                    onAudioSeekValueChanged = { message, seekValue -> /*TODO*/ },
                    onImageClicked = { message -> /*TODO*/ }
                )
            }
        }

        TextInput(
            modifier = Modifier
                .padding(20.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(Color.White)
                .fillMaxWidth()
                .align(Alignment.BottomEnd),
            message = "",
            onMessageChanged = { /*TODO*/ },
            onAttachmentClicked = { /*TODO*/ },
            onRecordClicked = { /*TODO*/ },
            onRecordLongPressed = { /*TODO*/ },
            onRecordReleased = { /*TODO*/ },
            onSendClicked = { /*TODO*/ }
        )
    }


}

@Composable
fun MessagesList(
    messages: List<Message>,
    modifier: Modifier = Modifier,
    onAudioSeekValueChanged: (Message, Float) -> Unit,
    onAudioStatusChanged: (Message, AudioStatus) -> Unit,
    onImageClicked: (Message) -> Unit
) {
    LazyColumn(
        reverseLayout = true,
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(
            count = messages.size,
            key = { index -> messages[index].id }
        ) { index ->
            val currentMessage = messages[index]
            val lastMessage: Message? = if (index > 0) messages[index - 1] else null

            if ("2" == currentMessage.from) {
                SentMessage(
                    currentMessage = currentMessage,
                    onAudioSeekValueChanged = { onAudioSeekValueChanged(currentMessage, it) },
                    onAudioStatusChanged = { onAudioStatusChanged(currentMessage, it) },
                    onImageClicked = { onImageClicked(currentMessage) }
                )
            } else {
                ReceivedMessage(
                    currentMessage = currentMessage,
                    onAudioSeekValueChanged = { onAudioSeekValueChanged(currentMessage, it) },
                    onAudioStatusChanged = { onAudioStatusChanged(currentMessage, it) },
                    onImageClicked = { onImageClicked(currentMessage) }
                )
            }

            val formattedDateSeparator = formatDateSeparator(
                currentMessage.timestamp,
                lastMessage?.timestamp
            )
            formattedDateSeparator?.let {
                Text(
                    text = formattedDateSeparator,
                    color = Color.White,
                    modifier = Modifier.padding(15.dp),
                    fontSize = 16.sp
                )
            }

            if (formattedDateSeparator == null) {
                Spacer(
                    modifier = Modifier
                        .height(
                            if (currentMessage.from == lastMessage?.from) 10.dp
                            else 15.dp
                        )
                )
            }
        }
    }
}

@Composable
fun ChatTopBar(
    onBackClicked: () -> Unit,
    onPersonClicked: () -> Unit,
    onMenuClicked: () -> Unit
) {
    TopAppBar(
        modifier = Modifier.height(80.dp),
        backgroundColor = Color.Transparent,
        elevation = 0.dp
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(79.dp)
                    .padding(PaddingValues(horizontal = 20.dp))
            ) {
                IconButton(onClick = onBackClicked, modifier = Modifier.padding(end = 20.dp)) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_back),
                        contentDescription = null,
                        tint = Color.White
                    )
                }

                Image(
                    painter = painterResource(id = R.drawable.test),
//                painter = rememberAsyncImagePainter(
//                    model = ImageRequest
//                        .Builder(LocalContext.current)
//                        .size(50, 50)
//                        .data(R.drawable.test)
//                        .build()
//                ),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 20.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { onPersonClicked() },
                    contentScale = ContentScale.Crop
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onPersonClicked() },
                ) {
                    Text(text = "Omar Alnaib", color = Color.White)
                    Text(text = "Online", color = Color.White, fontSize = 14.sp)
                }

                IconButton(onClick = onMenuClicked) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_menu),
                        contentDescription = null,
                        modifier = Modifier.padding(20.dp),
                        tint = Color.White
                    )
                }
            }
            Spacer(
                modifier = Modifier
                    .height(2.dp)
                    .fillMaxWidth()
                    .background(Color.White)
            )
        }

    }
}

@Composable
fun TextInput(
    message: String,
    onMessageChanged: (String) -> Unit,
    onAttachmentClicked: () -> Unit,
    onRecordClicked: () -> Unit,
    onRecordLongPressed: () -> Unit,
    onRecordReleased: () -> Unit,
    onSendClicked: () -> Unit,
    modifier: Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        IconButton(onClick = onAttachmentClicked) {
            Icon(
                painter = painterResource(id = R.drawable.ic_attachment),
                contentDescription = null,
                tint = Gray
            )
        }


        TextField(
            modifier = Modifier.weight(1f),
            value = message,
            onValueChange = onMessageChanged,
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            placeholder = {
                Text(text = stringResource(id = R.string.message_text_field_placeholder))
            }
        )

        Spacer(
            modifier = Modifier
                .height(25.dp)
                .width(1.dp)
                .background(Gray)
        )

        val interactionSource = remember { MutableInteractionSource() }
        val pressed by interactionSource.collectIsPressedAsState()

        if (pressed) {
            onRecordLongPressed()
            DisposableEffect(Unit) {
                onDispose {
                    onRecordReleased()
                }
            }
        }

        IconButton(
            onClick = onRecordClicked,
            interactionSource = interactionSource
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_record),
                contentDescription = null,
                tint = Orange
            )
        }

        IconButton(onClick = onSendClicked) {
            Icon(
                painter = painterResource(id = R.drawable.ic_send),
                contentDescription = null,
                tint = Green
            )
        }
    }
}

@Preview
@Composable
fun PreviewTopBar() {
    ChatTopBar({}, {}, {})
}

@Composable
fun SentMessage(
    currentMessage: Message,
    onAudioStatusChanged: (AudioStatus) -> Unit,
    onAudioSeekValueChanged: (Float) -> Unit,
    onImageClicked: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        TimeText(timestamp = currentMessage.timestamp)

        Column(
            modifier = Modifier
                .padding(end = 20.dp)
                .background(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF26DBB5), Color(0xFF71BCAC)),
                        Offset.Infinite, Offset.Infinite
                    ),
                    shape = RoundedCornerShape(
                        topStart = 15.dp,
                        topEnd = 15.dp,
                        bottomStart = 15.dp
                    )
                )
                .weight(1f)
                .padding(15.dp)

        ) {
            when (currentMessage.media?.type) {
                Media.Type.Image -> ImageMessageLayout(
                    onImageClicked = { onImageClicked() }
                )
                Media.Type.Audio -> AudioMessageLayout(
                    onSeekValueChanged = onAudioSeekValueChanged,
                    onStatusChanged = onAudioStatusChanged
                )
                else -> {}
            }
            currentMessage.body?.let {
                Text(
                    text = it,
                    color = Color.White,
                    modifier = Modifier.padding(top = if (currentMessage.media != null) 5.dp else 0.dp)
                )
            }
        }
    }
}

@Composable
fun ReceivedMessage(
    currentMessage: Message,
    onAudioStatusChanged: (AudioStatus) -> Unit,
    onAudioSeekValueChanged: (Float) -> Unit,
    onImageClicked: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier
                .padding(start = 20.dp)
                .background(
                    color = Color.DarkGray,
                    shape = RoundedCornerShape(
                        topEnd = 15.dp,
                        bottomStart = 15.dp,
                        bottomEnd = 15.dp
                    )
                )
                .weight(1f)
                .padding(15.dp)

        ) {
            when (currentMessage.media?.type) {
                Media.Type.Image -> ImageMessageLayout(
                    onImageClicked = onImageClicked
                )
                Media.Type.Audio -> AudioMessageLayout(
                    onStatusChanged = onAudioStatusChanged,
                    onSeekValueChanged = onAudioSeekValueChanged
                )
                else -> {}
            }

            currentMessage.body?.let {
                Text(
                    text = it,
                    color = Color.White,
                    modifier = Modifier.padding(top = if (currentMessage.media != null) 5.dp else 0.dp)
                )
            }
        }

        TimeText(timestamp = currentMessage.timestamp)
    }

}

@Composable
fun TimeText(timestamp: Long) {
    Text(
        text = timestamp.formatTimestamp(TWO_DIGIT_FORMAT),
        color = Color.White,
        modifier = Modifier.padding(20.dp)
    )
}

@Composable
fun ImageMessageLayout(
    onImageClicked: () -> Unit
) {
    Image(
        painter = rememberAsyncImagePainter(
            model = ImageRequest
                .Builder(LocalContext.current)
                .size(256, 200)
                .data(R.drawable.test)
                .build(),
            fallback = painterResource(id = R.drawable.ic_launcher_foreground)
        ),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(15.dp))
            .clickable { onImageClicked() }
    )
}

@Composable
fun AudioMessageLayout(
    onStatusChanged: (AudioStatus) -> Unit,
    onSeekValueChanged: (Float) -> Unit
) {
    var audioStatus by remember { mutableStateOf(AudioStatus.STOPPED) }
    val animatedValue by animateFloatAsState(targetValue = if (audioStatus == AudioStatus.PLAYING) 100f else 0f)

    Row(verticalAlignment = Alignment.CenterVertically) {

        AndroidView(
            modifier = Modifier
                .height(50.dp)
                .weight(1f),
            factory = { context ->
                AudioWaveView(context).apply {
                    this.minChunkHeight = 50
                    this.waveColor =
                        ResourcesCompat.getColor(resources, R.color.white, context.theme)
                    this.chunkWidth = 10
                    this.chunkRadius = 5
                    this.chunkSpacing = 15
                    this.onProgressListener = object : OnProgressListener {
                        override fun onProgressChanged(progress: Float, byUser: Boolean) {}

                        override fun onStartTracking(progress: Float) {
                            audioStatus = AudioStatus.PAUSED
                            onStatusChanged(audioStatus)
                        }

                        override fun onStopTracking(progress: Float) {
                            onSeekValueChanged(progress)
                        }
                    }
                }
            },
            update = {
                it.progress = animatedValue
            }
        )

        IconButton(onClick = {
            audioStatus =
                if (audioStatus == AudioStatus.STOPPED || audioStatus == AudioStatus.PAUSED) AudioStatus.PLAYING else AudioStatus.PAUSED
            onStatusChanged(audioStatus)
        }
        ) {
            Icon(
                imageVector = ImageVector
                    .vectorResource(
                        id = if (audioStatus == AudioStatus.PLAYING) R.drawable.ic_pause else R.drawable.ic_play
                    ),
                contentDescription = null,
                tint = Color.White
            )
        }

    }
}

enum class AudioStatus {
    STOPPED,
    PLAYING,
    PAUSED,
    ENDED
}