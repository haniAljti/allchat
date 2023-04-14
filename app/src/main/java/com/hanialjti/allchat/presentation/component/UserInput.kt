package com.hanialjti.allchat.presentation.component

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.swipeable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanialjti.allchat.R
import com.hanialjti.allchat.data.model.Attachment
import com.hanialjti.allchat.data.model.MessageItem
import com.hanialjti.allchat.data.model.ReplyingToMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import kotlin.math.roundToInt
import kotlin.time.toKotlinDuration

//@Preview
//@Composable
//fun TextInputWithText() {
//    TextInput(
//        message = "Hello",
//        onMessageChanged = { },
//        onOpenGallery = { },
//        onOpenCamera = { },
//        onSelectDocument = { },
//        onRecordClicked = { },
//        onRecordCancelled = { },
//        onRecordLongPressed = { },
//        onRecordReleased = { }
//    ) { }
//}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TextInput(
    modifier: Modifier = Modifier,
    message: String,
    attachment: Attachment? = null,
    attachmentButtonVisible: Boolean = true,
    recordButtonVisible: Boolean = true,
    replyingTo: MessageItem.MessageData?,
    onReplyToCleared: () -> Unit,
    onMessageChanged: (String) -> Unit,
    onOpenGallery: () -> Unit,
    onOpenCamera: () -> Unit,
    onSelectDocument: () -> Unit,
    onRecordClicked: () -> Unit,
    onRecordingCancelled: () -> Unit,
    onRecordingStarted: () -> Unit,
    onRecordingEnded: () -> Unit,
    onSendClicked: () -> Unit
) {

    Column(modifier = modifier) {

        AnimatedVisibility(visible = replyingTo != null) {
            replyingTo?.id?.let {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                        .clip(RoundedCornerShape(20))
                        .background(MaterialTheme.colorScheme.surface),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ReplyingTo(
                        message = ReplyingToMessage(
                            it,
                            replyingTo.senderName,
                            replyingTo.body,
                            replyingTo.attachment
                        ),
                        modifier = Modifier
                            .weight(1f)
                    ) { }
                    IconButton(onClick = { onReplyToCleared() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp),
                            contentDescription = null
                        )
                    }
                }
            }
        }

        val userInputState =
            rememberUserInputState(recordInitialValue = RecordingButtonState.Initial)
        val attachmentDeleteRecordingButton by remember { derivedStateOf { attachmentButtonVisible || userInputState.isRecording } }
        var showSelectAttachmentMenu by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        val haptic = LocalHapticFeedback.current
        val layoutDirection = LocalLayoutDirection.current

        Box {

            var size by remember { mutableStateOf(Size.Zero) }
            val sizePx = with(LocalDensity.current) { 55.dp.toPx() }
            val width = remember(size) {
                if (size.width == 0f) {
                    1f
                } else {
                    size.width - (sizePx)
                }
            }

            val anchors = mapOf(
                -width + (sizePx / 2) to RecordingButtonState.Cancel,
                0f to RecordingButtonState.Initial
            )

            val buttonMode by remember(message, attachment, recordButtonVisible) {
                derivedStateOf {
                    when {
                        !recordButtonVisible || message.isNotBlank() || attachment != null -> SendButtonMode.Send
                        recordButtonVisible -> SendButtonMode.Record
                        else -> SendButtonMode.None
                    }
                }
            }

            var textInputModifier = Modifier
                .onSizeChanged { size = Size(it.width.toFloat(), it.height.toFloat()) }
                .fillMaxWidth()

            if (buttonMode == SendButtonMode.Record) {
                textInputModifier = textInputModifier
                    .swipeable(
                        interactionSource = userInputState.interactionSource,
                        state = userInputState.swipeableState,
                        anchors = anchors,
                        reverseDirection = layoutDirection == LayoutDirection.Rtl,
                        thresholds = { _, _ -> FractionalThreshold(0.5f) },
                        orientation = Orientation.Horizontal,
                        enabled = !userInputState.thresholdReached
                    )

            }

            if (userInputState.isRecording) {
                textInputModifier = textInputModifier
                    .background(
                        MaterialTheme.colorScheme.secondaryContainer
                    )
            }

            if (userInputState.isRecording) {
                DisposableEffect(Unit) {
                    onRecordingStarted()

                    onDispose {
                        onRecordingEnded()
                        scope.launch {
                            userInputState.animateRecordingStateTo(RecordingButtonState.Initial)
                        }
                    }
                }
            }

            if (userInputState.thresholdReached) {
                LaunchedEffect(Unit) {
                    scope.launch {
                        println("Threshold reached")
                        onRecordingCancelled()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
            }

            if (!showSelectAttachmentMenu) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = textInputModifier.padding(horizontal = 10.dp, vertical = 5.dp)
                ) {


                    AnimatedContent(
                        targetState = userInputState.isRecording,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (it) {

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .width(100.dp)
                                    .background(Color.Transparent)
                            ) {
                                var ticks by remember { mutableStateOf(0) }
                                LaunchedEffect(Unit) {
                                    while (true) {
                                        delay(1000)
                                        ticks++
                                    }
                                }

                                val duration =
                                    Duration.ofSeconds(ticks.toLong()).toKotlinDuration()

                                Icon(
                                    painter = painterResource(id = R.drawable.ic_remove),
                                    modifier = Modifier
                                        .padding(horizontal = 25.dp)
                                        .size(24.dp),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )

                                Text(
                                    text = "Recording $duration",
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = modifier.weight(1f)
                                )

                            }

                        } else {
                            BasicTextField(
                                value = message,
                                onValueChange = onMessageChanged,
                                maxLines = 4,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 16.sp,
                                    color = Color.White
                                ),
                                modifier = Modifier.weight(1f),
                                cursorBrush = SolidColor(Color.White),
                                decorationBox = { innerTextField ->
                                    Row(
                                        modifier
                                            .clip(RoundedCornerShape(30))
                                            .background(MaterialTheme.colorScheme.secondaryContainer)
                                            .height(45.dp)
                                            .padding(start = 15.dp)
                                            .padding(vertical = 1.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(Modifier.weight(1f)) {
                                            if (message.isEmpty()) Text(
                                                stringResource(id = R.string.message_text_field_placeholder),
                                                style = LocalTextStyle.current.copy(
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    fontSize = 16.sp
                                                )
                                            )
                                            innerTextField()
                                        }
                                        if (attachmentDeleteRecordingButton) {
                                            IconButton(
                                                onClick = { showSelectAttachmentMenu = true },
                                                modifier = Modifier
                                                    .size(45.dp)
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_attachment),
                                                    modifier = Modifier.size(24.dp),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }


                    Spacer(modifier = Modifier.width(5.dp))

                    AnimatedContent(
                        targetState = buttonMode,
                        transitionSpec = { scaleIn() with scaleOut() },
                        modifier = Modifier
                            .shadow(elevation = 1.dp, shape = CircleShape)
                            .offset {
                                IntOffset(
                                    userInputState.swipeOffset.roundToInt(),
//                                swipeableState.offset.value.roundToInt(),
                                    0
                                )
                            }
                    ) {

                        val buttonModifier = Modifier
                            .clip(RoundedCornerShape(50))

                        val initialButtonSize by remember { mutableStateOf(45.dp) }

                        when (it) {
                            SendButtonMode.None -> {}
                            SendButtonMode.Send -> {
                                IconButton(
                                    onClick = onSendClicked,
                                    modifier = buttonModifier
                                        .shadow(elevation = 10.dp, shape = CircleShape)
                                        .background(MaterialTheme.colorScheme.secondary)
                                        .size(initialButtonSize)
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            id = R.drawable.ic_send
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.onSecondary
                                    )
                                }
                            }
                            SendButtonMode.Record -> {

                                val transition =
                                    updateTransition(
                                        targetState = userInputState.isRecording,
                                        label = ""
                                    )

                                val recordButtonSize by transition.animateDp(label = "") { recording ->
                                    if (recording) 60.dp else initialButtonSize
                                }

                                val recordButtonColor by transition.animateColor(label = "") { recording ->
                                    if (recording) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (userInputState.isRecording)
                                        Text(
                                            text = "< Swipe to delete",
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    IconButton(
                                        onClick = onRecordClicked,
                                        interactionSource = userInputState.interactionSource,
                                        modifier = buttonModifier
                                            .background(recordButtonColor)
                                            .size(recordButtonSize)

                                    ) {
                                        Icon(
                                            painter = painterResource(
                                                id = R.drawable.ic_record
                                            ),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                            tint = MaterialTheme.colorScheme.onSecondary
                                        )
                                    }
                                }

                            }
                        }
                    }
                }
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = showSelectAttachmentMenu,
                modifier = Modifier.fillMaxWidth(),
                enter = slideInHorizontally { -it },
                exit = slideOutHorizontally { -it }
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .height(62.dp)
                ) {
                    SelectAttachmentOption.all.forEach { option ->
                        OptionButton(
                            onClick = {
                                showSelectAttachmentMenu = false
                                when (option) {
                                    is SelectAttachmentOption.Document -> onSelectDocument()
                                    is SelectAttachmentOption.Gallery -> onOpenGallery()
                                    is SelectAttachmentOption.Camera -> onOpenCamera()
                                    else -> {}
                                }
                            },
                            iconRes = option.iconRes,
                            textRes = option.textRes
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionButton(
    onClick: () -> Unit,
    iconRes: Int,
    textRes: Int,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .background(Color.Transparent)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(id = iconRes),
                modifier = modifier,
                contentDescription = null,
                tint = Color.White
            )
            if (textRes != -1)
                Text(text = stringResource(id = textRes), color = Color.White, fontSize = 12.sp)
        }

    }
}

private sealed class SelectAttachmentOption(
    @DrawableRes val iconRes: Int,
    @StringRes val textRes: Int
) {
    object Document : SelectAttachmentOption(R.drawable.ic_document, R.string.document)
    object Gallery : SelectAttachmentOption(R.drawable.ic_gallery, R.string.gallery)
    object Camera : SelectAttachmentOption(R.drawable.ic_camera, R.string.camera)
    object Location : SelectAttachmentOption(R.drawable.ic_location, R.string.location)
    object Close : SelectAttachmentOption(R.drawable.ic_close, -1)

    companion object {
        val all = listOf(
            Document,
            Gallery,
            Camera,
            Location,
            Close
        )
    }
}

private enum class SendButtonMode { Send, Record, None }