package com.hanialjti.allchat.ui.screens

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.hanialjti.allchat.R
import com.hanialjti.allchat.component.*
import com.hanialjti.allchat.models.ContactImage
import com.hanialjti.allchat.models.UiAttachment
import com.hanialjti.allchat.models.UiMessage
import com.hanialjti.allchat.models.defaultAttachmentName
import com.hanialjti.allchat.models.state.rememberMediaPlayerState
import com.hanialjti.allchat.models.state.rememberMediaRecorderState
import com.hanialjti.allchat.sdk26AndUp
import com.hanialjti.allchat.ui.toImagePreviewScreen
import com.hanialjti.allchat.utils.*
import com.hanialjti.allchat.viewmodels.ChatViewModel
import com.hanialjti.allchat.viewmodels.defaultName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class, ExperimentalPermissionsApi::class)
@Composable
fun ChatScreen(
    navController: NavHostController,
    contactId: String,
    viewModel: ChatViewModel = hiltViewModel()
) {

    LaunchedEffect(Unit) { viewModel.setConversation(contactId, "user_1") }

    val messages =
        remember(viewModel) { viewModel.getMessages(contactId) }.collectAsLazyPagingItems()
    val uiState by remember(viewModel) { viewModel.uiState }.collectAsState()

    val bottomSheetState =
        rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    val coroutine = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner by rememberUpdatedState(newValue = LocalLifecycleOwner.current)
    val mediaPlayerState = rememberMediaPlayerState(mediaPlayer = MediaPlayer())
    val mediaRecorderState =
        rememberMediaRecorderState(mediaRecorder = sdk26AndUp { MediaRecorder(context) }
            ?: MediaRecorder())

    val recordAudioPermissionState = rememberPermissionState(
        permission = Manifest.permission.RECORD_AUDIO
    )

    val mediaPickerLauncher = context.mediaPickerLauncher(
        coroutineScope = coroutine,
        onImageSelected = viewModel::updateAttachment
    )

    val cameraLauncher = context.cameraLauncher(
        coroutineScope = coroutine,
        onImageTaken = viewModel::updateAttachment
    )

    val openPdfDocumentLauncher = context.openPdfDocumentLauncher(
        onPdfAttachmentSelected = {
            viewModel.updateAttachment(it)
            viewModel.sendMessage()
        }
    )

    ModalBottomSheetLayout(
        sheetContent = {
            OpenFilePickerSheetLayout(
                coroutine,
                bottomSheetState,
                mediaPickerLauncher,
                cameraLauncher,
                openPdfDocumentLauncher
            )
        },
        sheetState = bottomSheetState,
        sheetShape = RoundedCornerShape(topEndPercent = 15, topStartPercent = 15)
    ) {
        DisposableEffect(
            Box {
                Column {
                    ChatTopBar(
                        name = uiState.name,
                        status = uiState.status?.asString(),
                        image = uiState.image,
                        onBackClicked = { navController.popBackStack() },
                        onPersonClicked = { /*TODO*/ },
                        onMenuClicked = { /*TODO*/ }
                    )


                    MessagesList(
                        messages = messages,
                        trackPositions = uiState.trackPositions,
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        owner = "user_1",
                        onResumeAudio = { recording ->
                            mediaPlayerState.playMedia(
                                recording,
                                uiState.trackPositions[recording.cacheUri]
                            )
                            recording.cacheUri?.let { viewModel.updateTrackPosition(it, 0) }
                        },
                        onPauseAudio = { recording ->
                            val stoppedAt = mediaPlayerState.pauseMedia()
                            recording.cacheUri?.let {
                                viewModel.updateTrackPosition(
                                    it,
                                    stoppedAt
                                )
                            }
                        },
                        onAudioSeekValueChanged = { recording, seekValue ->
                            recording.cacheUri?.let {
                                viewModel.updateTrackPosition(
                                    it,
                                    seekValue
                                )
                            }
                        },
                        onPdfClicked = { pdf ->
                            val uri = Uri.parse(pdf.cacheUri)
                            val openPdfIntent = Intent(Intent.ACTION_VIEW)
                            openPdfIntent.setDataAndType(uri, "application/pdf")
                            openPdfIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

                            val chooserIntent = Intent
                                .createChooser(
                                    openPdfIntent,
                                    context.getString(R.string.choose_pdf_title)
                                )

                            try {
                                context.startActivity(chooserIntent)
                            } catch (e: ActivityNotFoundException) {
                                e.printStackTrace()
                            }
                        },
                        onImageClicked = { message ->
                            navController.toImagePreviewScreen(message.id)
                        },
                        activeMessage = mediaPlayerState.activeRecording.value,
                        onAttachmentDownloaded = { message, uri ->
                            viewModel.saveMessageContentUri(message, uri)
                        }
                    )

                }

                TextInput(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                        .align(Alignment.BottomEnd)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color.White),
                    message = uiState.textInput,
                    attachment = uiState.attachment,
                    onMessageChanged = viewModel::updateTextInput,
                    onAttachmentClicked = { coroutine.launch { bottomSheetState.show() } },
                    onRemoveAttachmentClicked = { viewModel.updateAttachment(null) },
                    onRecordClicked = {
                        when (recordAudioPermissionState.status) {
                            is PermissionStatus.Denied -> {
                                recordAudioPermissionState.launchPermissionRequest()
                            }
                            else -> {}
                        }
                    },
                    onRecordLongPressed = {
                        when (recordAudioPermissionState.status) {
                            is PermissionStatus.Denied -> {
                                recordAudioPermissionState.launchPermissionRequest()
                            }
                            PermissionStatus.Granted -> {
                                mediaRecorderState.startRecording()
                            }
                        }
                    },
                    onRecordReleased = {

                        if (recordAudioPermissionState.status == PermissionStatus.Granted) {
                            coroutine.launch(Dispatchers.IO) {
                                try {
                                    val recording = mediaRecorderState.stopRecording()
                                    viewModel.updateAttachment(recording)
                                    viewModel.sendMessage()
                                } catch (e: java.io.IOException) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    },
                    onSendClicked = viewModel::sendMessage
                )
            },
            lifecycleOwner
        ) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> {
                        mediaPlayerState.stopMedia()
                        mediaRecorderState.stopMediaRecorder()
                    }
                    Lifecycle.Event.ON_DESTROY -> {
                        mediaPlayerState.releasePlayer()
                        mediaRecorderState.releaseRecorder()
                    }
                    else -> {}
                }
            }

            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                mediaPlayerState.releasePlayer()
            }
        }

    }
}

@Composable
fun Context.mediaPickerLauncher(
    coroutineScope: CoroutineScope,
    onImageSelected: (UiAttachment.Image) -> Unit
) = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->

    if (uri != null) {

        coroutineScope.launch {
            val imageName = defaultAttachmentName
            val savedImageUri = saveImageToInternalStorage(uri, imageName)
            val attachment = UiAttachment.Image(
                null,
                imageName,
                savedImageUri.path,
                0,
                null,
                null
            )

            onImageSelected(attachment)
        }

        Log.d("PhotoPicker", "Selected URI: $uri")
    } else {
        Log.d("PhotoPicker", "No media selected")
    }
}


@Composable
fun Context.openPdfDocumentLauncher(
    onPdfAttachmentSelected: (UiAttachment.Pdf) -> Unit
) = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->

    val cachedImageName = defaultAttachmentName

    if (uri != null) {
        contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )

        onPdfAttachmentSelected(
            UiAttachment.Pdf(
                null,
                cachedImageName,
                uri.path,
                null
            )
        )

        Log.d("PhotoPicker", "Selected URI: $uri")
    } else {
        Log.d("PhotoPicker", "No media selected")
    }
}

@Composable
fun Context.cameraLauncher(
    coroutineScope: CoroutineScope,
    onImageTaken: (UiAttachment.Image) -> Unit
) = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->

    val cachedImageName = defaultAttachmentName
    coroutineScope.launch {
        bitmap?.let {
            val uri = saveBitmapToInternalStorage(bitmap, cachedImageName)
            onImageTaken(
                UiAttachment.Image(
                    null,
                    cachedImageName,
                    uri.path,
                    0,
                    null,
                    null
                )
            )
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessagesList(
    messages: LazyPagingItems<UiMessage>,
    trackPositions: Map<String, Int>,
    modifier: Modifier = Modifier,
    owner: String,
    onAudioSeekValueChanged: (UiAttachment.Recording, Int) -> Unit,
    onResumeAudio: (UiAttachment.Recording) -> Unit,
    onPauseAudio: (UiAttachment.Recording) -> Unit,
    onPdfClicked: (UiAttachment.Pdf) -> Unit,
    onImageClicked: (UiMessage) -> Unit,
    onAttachmentDownloaded: (UiMessage, String) -> Unit,
    activeMessage: UiAttachment.Recording?
) {

    messages.loadState.append

    val coroutine = rememberCoroutineScope()
    val context = LocalContext.current

    val messageListState = rememberLazyListState()

    LaunchedEffect(messages.loadState) {
        if (messageListState.firstVisibleItemIndex < 10) {
            messageListState.animateScrollToItem(0)
        }
    }

    LazyColumn(
        reverseLayout = true,
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 90.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        state = messageListState
    ) {
        items(
            count = messages.itemCount,
            key = { messages[it]?.id ?: it }
        ) { index ->
            val currentMessage = messages[index]
            val lastMessage = if (index < messages.itemCount - 1) messages[index + 1] else null

            LaunchedEffect(Unit) {

                when (val attachment = currentMessage?.attachment) {
                    is UiAttachment.Recording -> {
                        if (attachment.url != null && attachment.cacheUri == null) {
                            coroutine.launch {
                                val uri = context.saveAttachmentToInternalStorage(attachment)
                                uri.path?.let { onAttachmentDownloaded(currentMessage, it) }
                            }
                        }
                    }
                    is UiAttachment.Pdf -> {
                        if (attachment.url != null && attachment.cacheUri == null) {
                            coroutine.launch {
                                val uri = context.saveAttachmentToInternalStorage(attachment)
                                uri.path?.let { onAttachmentDownloaded(currentMessage, it) }
                            }
                        }
                    }
                    else -> {}
                }
            }

            Box(
                modifier = Modifier
                    .padding(
                        top = messageTopPadding(
                            lastMessage = lastMessage,
                            currentMessage = currentMessage
                        )
                    )
                    .animateItemPlacement(

                    )
            ) {

                val lastTrackPosition = if (currentMessage?.attachment is UiAttachment.Recording)
                    trackPositions[currentMessage.attachment.cacheUri] else 0

                if (currentMessage == null) PlaceHolderMessage()
                else {
                    if (owner == currentMessage.from) {
                        SentMessage(
                            message = currentMessage,
                            lastMessageFromSameSender = lastMessage?.from == currentMessage.from,
                            onAudioSeekValueChanged = {
                                onAudioSeekValueChanged(
                                    currentMessage.attachment as UiAttachment.Recording,
                                    it
                                )
                            },
                            onPauseAudio = { onPauseAudio(currentMessage.attachment as UiAttachment.Recording) },
                            onResumeAudio = { onResumeAudio(currentMessage.attachment as UiAttachment.Recording) },
                            onPdfClicked = { onPdfClicked(currentMessage.attachment as UiAttachment.Pdf) },
                            onImageClicked = { onImageClicked(currentMessage) },
                            isActiveMessage = activeMessage == currentMessage.attachment,
                            lastTrackPosition = lastTrackPosition ?: 0
                        )
                    } else {
                        ReceivedMessage(
                            message = currentMessage,
                            lastMessageFromSameSender = lastMessage?.from == currentMessage.from,
                            onAudioSeekValueChanged = {
                                onAudioSeekValueChanged(
                                    currentMessage.attachment as UiAttachment.Recording,
                                    it
                                )
                            },
                            onPauseAudio = { onPauseAudio(currentMessage.attachment as UiAttachment.Recording) },
                            onResumeAudio = { onResumeAudio(currentMessage.attachment as UiAttachment.Recording) },
                            onPdfClicked = { onPdfClicked(currentMessage.attachment as UiAttachment.Pdf) },
                            onImageClicked = { onImageClicked(currentMessage) },
                            isActiveMessage = activeMessage == currentMessage.attachment,
                            lastTrackPosition = lastTrackPosition ?: 0
                        )
                    }
                }
            }

            if (currentMessage != null && shouldDisplayDateSeparator(
                    lastMessage?.timestamp?.asLocalDate(),
                    currentMessage.timestamp.asLocalDate()
                )
            ) {
                val currentMessageDateText =
                    currentMessage.timestamp.asLocalDateTime().getDateText()

                Box(
                    modifier = Modifier
                        .padding(10.dp)
                        .padding(horizontal = 5.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = currentMessageDateText.asSeparator(),
                        color = Color.White,
                        modifier = Modifier,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun messageTopPadding(lastMessage: UiMessage?, currentMessage: UiMessage?): Dp {
    return if (lastMessage == null || lastMessage.from == currentMessage?.from) 3.dp else 10.dp
}

@Composable
fun ChatTopBar(
    name: String?,
    status: String?,
    image: ContactImage? = null,
    onBackClicked: () -> Unit,
    onPersonClicked: () -> Unit,
    onMenuClicked: () -> Unit
) {
    TopAppBar(
        modifier = Modifier
            .height(80.dp)
            .fillMaxWidth(),
        backgroundColor = Color.Transparent,
        elevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
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

                image?.AsImage(modifier = Modifier.size(40.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 5.dp)
                        .clickable { onPersonClicked() },
                ) {
                    Text(text = name ?: defaultName, color = Color.White)
                    AnimatedVisibility(visible = status != null) {
                        status?.let { Text(text = it, color = Color.White, fontSize = 14.sp) }
                    }
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
fun MessageTime(timestamp: Long, modifier: Modifier = Modifier) {
    Text(
        text = timestamp.formatTimestamp(TWO_DIGIT_FORMAT),
        color = Color.White,
        modifier = modifier
    )
}

@Composable
fun imageBottomCornerRadius(isMessageBodyEmpty: Boolean) = if (isMessageBodyEmpty) 15.dp else 5.dp

@Composable
fun AudioPlayBackButton(
    modifier: Modifier = Modifier,
    @DrawableRes imageRes: Int,
    onButtonClicked: () -> Unit
) {
    IconButton(onClick = onButtonClicked, modifier = modifier) {
        Icon(
            imageVector = ImageVector
                .vectorResource(id = imageRes),
            contentDescription = null,
            tint = Color.White
        )
    }
}




