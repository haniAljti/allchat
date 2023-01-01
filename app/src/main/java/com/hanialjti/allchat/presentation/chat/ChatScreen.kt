package com.hanialjti.allchat.presentation.chat

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.hanialjti.allchat.R
import com.hanialjti.allchat.common.utils.*
import com.hanialjti.allchat.data.model.MessageItem
import com.hanialjti.allchat.data.model.MessageStatus
import com.hanialjti.allchat.di.getViewModel
import com.hanialjti.allchat.presentation.component.*
import com.hanialjti.allchat.presentation.conversation.ContactImage
import com.hanialjti.allchat.presentation.ui.toInviteUsersScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf
import timber.log.Timber
import java.time.LocalDateTime
import java.util.UUID


@OptIn(ExperimentalMaterialApi::class, ExperimentalPermissionsApi::class)
@Composable
fun ChatScreen(
    navController: NavHostController,
    contactId: String,
    isGroupChat: Boolean,
    viewModel: ChatViewModel = getViewModel(parameters = { parametersOf(contactId, isGroupChat) })
) {

    val uiState by remember(viewModel) { viewModel.uiState }.collectAsState()
    val messages = remember(viewModel) { viewModel.messages }.collectAsLazyPagingItems()
    val showChatMenu = remember { mutableStateOf(false) }

    val bottomSheetState =
        rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    val coroutine = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner by rememberUpdatedState(newValue = LocalLifecycleOwner.current)
    val mediaPlayerState = rememberMediaPlayerState()
    val mediaRecorderState = rememberMediaRecorderState()

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
                        chatMenuExpanded = showChatMenu.value,
                        onChatMenuDismissed = { showChatMenu.value = false },
                        inviteUsersOptionEnabled = isGroupChat,
                        onInviteUsersClicked = { navController.toInviteUsersScreen(contactId) },
                        onBackClicked = { navController.popBackStack() },
                        onPersonClicked = { /*TODO*/ },
                        onMenuClicked = { showChatMenu.value = true }
                    )

                    MessagesList(
                        messages = messages,
                        trackPositions = uiState.trackPositions,
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
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
//                            navController.toImagePreviewScreen(message.id)
                        },
                        activeMessage = mediaPlayerState.activeRecording.value,
                        onAttachmentDownloaded = { message, uri ->
                            viewModel.saveMessageContentUri(message, uri)
                        },
                        lastReadMessage = viewModel::updateLastReadMessage
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
                        viewModel.setAllMessagesAsRead()
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
    onImageSelected: (Attachment.Image) -> Unit
) = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->

    if (uri != null) {

        coroutineScope.launch {
            val imageName = defaultAttachmentName
            val savedImageUri = saveImageToInternalStorage(uri, imageName)
            val attachment = Attachment.Image(
                null,
                imageName,
                savedImageUri.path,
                0,
                null,
                null
            )

            onImageSelected(attachment)
        }

        Timber.d("Selected URI: $uri")
    } else {
        Timber.d("No media selected")
    }
}


@Composable
fun Context.openPdfDocumentLauncher(
    onPdfAttachmentSelected: (Attachment.Pdf) -> Unit
) = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->

    val cachedImageName = defaultAttachmentName

    if (uri != null) {
        contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )

        onPdfAttachmentSelected(
            Attachment.Pdf(
                null,
                cachedImageName,
                uri.path,
                null
            )
        )

        Timber.d("Selected URI: $uri")
    } else {
        Timber.d("No media selected")
    }
}

@Composable
fun Context.cameraLauncher(
    coroutineScope: CoroutineScope,
    onImageTaken: (Attachment.Image) -> Unit
) = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->

    val cachedImageName = defaultAttachmentName
    coroutineScope.launch {
        bitmap?.let {
            val uri = saveBitmapToInternalStorage(bitmap, cachedImageName)
            onImageTaken(
                Attachment.Image(
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


@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun MessagesList(
    messages: LazyPagingItems<MessageItem>,
    trackPositions: Map<String, Int>,
    modifier: Modifier = Modifier,
    onAudioSeekValueChanged: (Attachment.Recording, Int) -> Unit,
    onResumeAudio: (Attachment.Recording) -> Unit,
    onPauseAudio: (Attachment.Recording) -> Unit,
    onPdfClicked: (Attachment.Pdf) -> Unit,
    onImageClicked: (MessageItem.MessageData) -> Unit,
    onAttachmentDownloaded: (MessageItem.MessageData, String) -> Unit,
    activeMessage: Attachment.Recording?,
    lastReadMessage: (MessageItem.MessageData) -> Unit
) {

    val coroutine = rememberCoroutineScope()
    val context = LocalContext.current

    val lastMessageNotSentByMe: MessageItem.MessageData? by remember(messages) {
        derivedStateOf { messages.itemSnapshotList.firstOrNull { it is MessageItem.MessageData && !it.isFromMe() } as? MessageItem.MessageData }
    }

    LaunchedEffect(lastMessageNotSentByMe) {
        lastMessageNotSentByMe?.let {
            if (it.status == MessageStatus.Delivered || it.status == MessageStatus.Sent) {
                lastReadMessage(it)
            }
        }
    }

    val messageListState = rememberLazyListState()

    val firstMessagesVisible by remember { derivedStateOf { messageListState.firstVisibleItemIndex < 25 } }

    LaunchedEffect(messages.itemCount) {
        if (messages.loadState.prepend is LoadState.NotLoading && !messageListState.isScrollInProgress)
            if (messageListState.firstVisibleItemIndex < 10) {
                messageListState.animateScrollToItem(0)
            }
    }

    Box(modifier = modifier) {
        LazyColumn(
            reverseLayout = true,
            modifier = modifier.align(Alignment.BottomCenter),
            contentPadding = PaddingValues(bottom = 90.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            state = messageListState
        ) {

            items(
                messages.itemCount,
                key = { index ->
                    messages[index]?.itemId ?: UUID.randomUUID().toString()
                }
            ) { index ->

                when (val currentMessage = messages[index]) {
                    null -> PlaceHolderMessage()
                    is MessageItem.MessageData -> {
                        val nextMessage =
                            if (index < messages.itemCount.minus(1)) messages[index + 1] else null
                        val previousMessage = if (index > 0) messages[index - 1] else null



                        LaunchedEffect(Unit) {

                            when (val attachment = currentMessage.attachment) {
                                is Attachment.Recording -> {
                                    if (attachment.url != null && attachment.cacheUri == null) {
                                        coroutine.launch {
                                            val uri =
                                                context.saveAttachmentToInternalStorage(attachment)
                                            uri.path?.let {
                                                onAttachmentDownloaded(
                                                    currentMessage,
                                                    it
                                                )
                                            }
                                        }
                                    }
                                }
                                is Attachment.Pdf -> {
                                    if (attachment.url != null && attachment.cacheUri == null) {
                                        coroutine.launch {
                                            val uri =
                                                context.saveAttachmentToInternalStorage(attachment)
                                            uri.path?.let {
                                                onAttachmentDownloaded(
                                                    currentMessage,
                                                    it
                                                )
                                            }
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
                                        lastMessage = nextMessage,
                                        currentMessage = currentMessage
                                    )
                                )
                                .animateItemPlacement()
                        ) {

                            val lastTrackPosition =
                                if (currentMessage.attachment is Attachment.Recording)
                                    trackPositions[currentMessage.attachment.cacheUri] else 0


                            if (currentMessage.isFromMe()) {
                                SentMessage(
                                    message = currentMessage,
                                    nextMessage = nextMessage,
                                    previousMessage = previousMessage,
                                    onAudioSeekValueChanged = {
                                        onAudioSeekValueChanged(
                                            currentMessage.attachment as Attachment.Recording,
                                            it
                                        )
                                    },
                                    onPauseAudio = { onPauseAudio(currentMessage.attachment as Attachment.Recording) },
                                    onResumeAudio = { onResumeAudio(currentMessage.attachment as Attachment.Recording) },
                                    onPdfClicked = { onPdfClicked(currentMessage.attachment as Attachment.Pdf) },
                                    onImageClicked = { onImageClicked(currentMessage) },
                                    isActiveMessage = activeMessage == currentMessage.attachment,
                                    lastTrackPosition = lastTrackPosition ?: 0
                                )
                            } else {
                                ReceivedMessage(
                                    message = currentMessage,
                                    nextMessage = nextMessage,
                                    previousMessage = previousMessage,
                                    onAudioSeekValueChanged = {
                                        onAudioSeekValueChanged(
                                            currentMessage.attachment as Attachment.Recording,
                                            it
                                        )
                                    },
                                    onPauseAudio = { onPauseAudio(currentMessage.attachment as Attachment.Recording) },
                                    onResumeAudio = { onResumeAudio(currentMessage.attachment as Attachment.Recording) },
                                    onPdfClicked = { onPdfClicked(currentMessage.attachment as Attachment.Pdf) },
                                    onImageClicked = { onImageClicked(currentMessage) },
                                    isActiveMessage = activeMessage == currentMessage.attachment,
                                    lastTrackPosition = lastTrackPosition ?: 0
                                )
                            }

                        }
                    }
                    is MessageItem.MessageDateSeparator -> {
                        Box(
                            modifier = Modifier
                                .padding(10.dp)
                                .padding(horizontal = 5.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = currentMessage.date.asSeparator(),
                                color = MaterialTheme.colors.primary,
                                modifier = Modifier,
                                fontSize = 16.sp
                            )
                        }
                    }
                    is MessageItem.NewMessagesSeparator -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(
                                modifier = Modifier
                                    .padding(10.dp)
                                    .height(1.dp)
                                    .weight(1f)
                                    .background(MaterialTheme.colors.primary)
                            )
                            Box(
                                modifier = Modifier
                                    .padding(10.dp)
                                    .padding(horizontal = 5.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = currentMessage.date.asString(),
                                    color = MaterialTheme.colors.primary,
                                    modifier = Modifier,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(
                                modifier = Modifier
                                    .padding(10.dp)
                                    .height(1.dp)
                                    .weight(1f)
                                    .background(MaterialTheme.colors.primary)
                            )
                        }
                    }
                }
            }


            item {
                AnimatedVisibility(visible = messages.loadState.append is LoadState.Loading) {
                    CircularProgressIndicator()
                }
            }

        }

        AnimatedVisibility(
            visible = !firstMessagesVisible,
            enter = scaleIn(),
            exit = scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 90.dp, end = 20.dp)
                .size(40.dp)
        ) {
            IconButton(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White),
                onClick = { coroutine.launch { messageListState.scrollToItem(0) } }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_down),
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
fun messageTopPadding(lastMessage: MessageItem?, currentMessage: MessageItem?): Dp {
    return if (lastMessage is MessageItem.MessageData && currentMessage is MessageItem.MessageData) {
        if (lastMessage.senderId == currentMessage.senderId) 3.dp else 10.dp
    } else 3.dp
}

@Composable
fun ChatTopBar(
    name: String?,
    status: String?,
    image: ContactImage? = null,
    chatMenuExpanded: Boolean,
    onChatMenuDismissed: () -> Unit,
    inviteUsersOptionEnabled: Boolean,
    onInviteUsersClicked: () -> Unit,
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
                        tint = MaterialTheme.colors.primary
                    )
                }

                image?.AsImage(modifier = Modifier.size(40.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 5.dp)
                        .clickable { onPersonClicked() },
                ) {
                    Text(text = name ?: defaultName, color = MaterialTheme.colors.primary)
                    AnimatedVisibility(visible = status != null) {
                        status?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colors.primary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Box {
                    IconButton(onClick = onMenuClicked) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_menu),
                            contentDescription = null,
                            modifier = Modifier.padding(20.dp),
                            tint = MaterialTheme.colors.primary
                        )
                    }

                    DropdownMenu(
                        modifier = Modifier
                            .width(200.dp)
                            .background(
                                color = MaterialTheme.colors.background,
                                shape = RoundedCornerShape(15.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = Color.White,
                                shape = RoundedCornerShape(15.dp)
                            ),
                        expanded = chatMenuExpanded,
                        onDismissRequest = onChatMenuDismissed
                    ) {

                        if (inviteUsersOptionEnabled) {
                            DropdownMenuItem(onClick = onInviteUsersClicked) {
                                Text("Invite Users")
                            }
                        }

                    }
                }
            }
            Spacer(
                modifier = Modifier
                    .height(2.dp)
                    .fillMaxWidth()
                    .background(MaterialTheme.colors.primary)
            )
        }

    }
}


@Composable
fun MessageTime(timestamp: LocalDateTime, modifier: Modifier = Modifier) {
    Text(
        text = timestamp.asString(TWO_DIGIT_FORMAT),
        color = MaterialTheme.colors.primary,
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