package com.hanialjti.allchat.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.hanialjti.allchat.R
import com.hanialjti.allchat.models.Attachment
import com.hanialjti.allchat.presentation.ui.theme.Gray
import com.hanialjti.allchat.presentation.ui.theme.Green
import kotlinx.coroutines.delay
import java.time.Duration
import kotlin.time.toKotlinDuration

@Composable
fun TextInput(
    modifier: Modifier = Modifier,
    message: String,
    attachment: Attachment? = null,
    sendButtonEnabled: Boolean = true,
    attachmentButtonVisible: Boolean = true,
    recordButtonVisible: Boolean = true,
    onMessageChanged: (String) -> Unit,
    onAttachmentClicked: () -> Unit,
    onRemoveAttachmentClicked: () -> Unit = {  },
    onRecordClicked: () -> Unit,
    onRecordLongPressed: () -> Unit,
    onRecordReleased: () -> Unit,
    onSendClicked: () -> Unit
) {

    Column(modifier = modifier) {

        AnimatedVisibility(visible = attachment != null) {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                when (attachment) {
                    is Attachment.Image -> {

                        Image(
                            painter = rememberAsyncImagePainter(attachment.cacheUri),
                            contentScale = ContentScale.Crop,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(1.dp)
                                .clip(RoundedCornerShape(15.dp))
                        )

                        IconButton(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                                .background(Color.DarkGray, shape = CircleShape),
                            onClick = onRemoveAttachmentClicked) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_remove),
                                modifier = Modifier.size(24.dp),
                                tint = Color.White,
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        }


        Row(verticalAlignment = Alignment.CenterVertically) {
            if (attachmentButtonVisible) {
                IconButton(onClick = onAttachmentClicked) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_attachment),
                        modifier = Modifier.size(24.dp),
                        contentDescription = null,
                        tint = Color.Black
                    )
                }
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
                DisposableEffect(Unit) {
                    onRecordLongPressed()
                    onDispose {
                        onRecordReleased()
                    }
                }
            }

            AnimatedVisibility(recordButtonVisible && attachment == null) {

                val transition = updateTransition(targetState = pressed, label = "")

                val size by transition.animateDp(label = "") { recording ->
                    if (recording) 70.dp else 50.dp
                }

                val color by transition.animateColor(label = "") { recording ->
                    if (recording) Green else Color.Transparent
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier) {
                    AnimatedVisibility(visible = pressed) {
                        if (pressed) {
                            var ticks by remember { mutableStateOf(0) }
                            LaunchedEffect(Unit) {
                                while (true) {
                                    delay(1000)
                                    ticks++
                                }
                            }

                            val duration = Duration.ofSeconds(ticks.toLong()).toKotlinDuration()

                            Text(text = "Recording $duration", modifier = Modifier.padding(10.dp))
                        }
                    }

                    IconButton(
                        onClick = onRecordClicked,
                        interactionSource = interactionSource,
                        modifier = Modifier
                            .background(color = color, shape = CircleShape)
                            .size(size)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_record),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = Color.Black
                        )
                    }
                }


            }

            IconButton(onClick = onSendClicked, enabled = sendButtonEnabled) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_send),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.Black
                )
            }
        }
    }


}