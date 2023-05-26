package com.hanialjti.allchat.presentation.chat

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.hanialjti.allchat.R
import com.hanialjti.allchat.data.model.Attachment
import com.hanialjti.allchat.data.model.Media
import com.hanialjti.allchat.data.model.MessageItem
import com.hanialjti.allchat.data.model.MessageStatus
import com.hanialjti.allchat.data.remote.model.DownloadProgress
import com.hanialjti.allchat.data.remote.model.UploadProgress
import com.hanialjti.allchat.di.getViewModel
import com.hanialjti.allchat.presentation.component.*
import com.hanialjti.allchat.presentation.conversation.ContactImage
import com.hanialjti.allchat.presentation.preview_attachment.PreviewAndSendAttachment
import com.hanialjti.allchat.presentation.ui.toChatDetailsScreen
import com.hanialjti.allchat.presentation.ui.toImagePreviewScreen
import com.hanialjti.allchat.presentation.ui.toInviteUsersScreen
import com.hanialjti.allchat.presentation.ui.toUserDetailsScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf
import java.io.File


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ChatScreen(
    contactId: String,
    navController: NavHostController,
    viewModel: ChatViewModel = getViewModel(parameters = { parametersOf(contactId) })
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val messages = viewModel.messages.collectAsLazyPagingItems()

    val coroutine = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner by rememberUpdatedState(LocalLifecycleOwner.current)
    val mediaPlayerState = rememberMediaPlayerState()
    val mediaRecorderState = rememberMediaRecorderState()

    val recordAudioPermissionState = rememberPermissionState(
        permission = Manifest.permission.RECORD_AUDIO
    )

    val mediaPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let {
                viewModel.updateAttachment(
                    Media(
                        cacheUri = uri.toString(),
                        type = Attachment.Type.Image
                    )
                )
            }
        }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSaved ->
        if (isSaved)
            viewModel.updateAttachment(
                Media(
                    type = Attachment.Type.Image,
                    cacheUri = uiState.lastCreatedTempFile?.path
                )
            )
        else viewModel.deleteCameraTempFile()
    }

    val openPdfDocumentLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->

            if (uri != null) {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                viewModel.updateAttachment(
                    Media(
                        cacheUri = uri.toString(),
                        type = Attachment.Type.Document,
                    )
                )
            }

        }

    BackHandler(enabled = uiState.attachment != null) {
        viewModel.updateAttachment(null)
    }

    DisposableEffect(
        Box(modifier = Modifier.imePadding()) {
            Column {
                ChatTopBar(
                    name = uiState.name,
                    status = uiState.status?.asString(),
                    image = uiState.image,
                    onBackClicked = { navController.popBackStack() },
                    onPersonClicked = {
                        if (uiState.isGroupChat) navController.toChatDetailsScreen(contactId)
                        else navController.toUserDetailsScreen(contactId)
                    },
                    onMenuClicked = { viewModel.updateIsChatMenuVisible(true) }
                ) {
                    DropdownMenu(
                        modifier = Modifier
                            .width(200.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ),
                        expanded = uiState.isChatMenuVisible,
                        onDismissRequest = { viewModel.updateIsChatMenuVisible(false) }
                    ) {

                        if (uiState.isGroupChat) {
                            DropdownMenuItem(
                                onClick = { navController.toInviteUsersScreen(contactId) },
                                text = { Text("Invite Users") }
                            )
                        } else {
                            DropdownMenuItem(
                                onClick = {
                                    if (uiState.isBlocked) viewModel.unblockUser()
                                    else viewModel.blockUser()
                                },
                                text = { Text(if (uiState.isBlocked) "Unblock user" else "Block user") }
                            )
                        }

                    }
                }

                MessagesList(
                    messages = messages,
                    messageListState = messages.rememberLazyListState(),
                    uploadProgressMap = uiState.uploadProgresses,
                    downloadProgressMap = uiState.downloadProgresses,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    onResumeAudio = { message, seekValue ->
                        if ((message.attachment as Media).cacheUri == null) {
                            viewModel.downloadAttachment(message)
                        } else {
                            mediaPlayerState.playMedia(
                                message.attachment,
                                seekValue
                            )
                        }
                    },
                    onPauseAudio = { mediaPlayerState.pauseMedia() },
                    onDocumentClicked = { message ->
                        if ((message.attachment as Media).cacheUri == null) {
                            viewModel.downloadAttachment(message)
                        } else {
                            context.openDocumentWithChooser(message.attachment)
                        }
                    },
                    onImageClicked = { messageId ->
                        navController.toImagePreviewScreen(messageId)
                    },
                    currentlyPlaying = mediaPlayerState.activeRecording,
                    lastReadMessage = viewModel::updateLastReadMessage,
                    replyTo = viewModel::updateReplyingTo
                )

            }

            TextInput(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomEnd),
                message = uiState.textInput,
                attachment = uiState.attachment,
                onMessageChanged = viewModel::updateTextInput,
                onOpenGallery = {
                    mediaPickerLauncher.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
                onOpenCamera = {
                    //TODO handle null file
                    val tempFile = viewModel.createNewTempFile(".jpg")
                    viewModel.updateTempFile(tempFile)
                    val fileUri = tempFile?.let {
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            it
                        )
                    }
                    cameraLauncher.launch(fileUri)
                },
                onSelectDocument = { openPdfDocumentLauncher.launch(arrayOf("application/*")) },
                onRecordClicked = {
                    when (recordAudioPermissionState.status) {
                        is PermissionStatus.Denied -> {
                            recordAudioPermissionState.launchPermissionRequest()
                        }

                        else -> {}
                    }
                },
                onRecordingCancelled = { mediaRecorderState.cancelRecording() },
                onRecordingStarted = {
                    when (recordAudioPermissionState.status) {
                        is PermissionStatus.Denied -> {
                            recordAudioPermissionState.launchPermissionRequest()
                        }

                        PermissionStatus.Granted -> {
                            //TODO handle null file
                            val tempFile = viewModel.createNewTempFile(".m4a")
                            viewModel.updateTempFile(tempFile)
                            tempFile?.let { mediaRecorderState.startRecording(it) }
                        }
                    }
                },
                onRecordingEnded = {
                    if (recordAudioPermissionState.status == PermissionStatus.Granted) {
                        coroutine.launch {
                            try {

                                val recording = mediaRecorderState.stopRecording() as Media
                                viewModel.updateAttachment(
                                    recording.copy(
                                        cacheUri = uiState.lastCreatedTempFile?.path,
                                        type = Attachment.Type.Audio
                                    )
                                )
                                viewModel.sendMessage()
                            } catch (e: java.io.IOException) {
                                e.printStackTrace()
                            }
                        }
                    }
                },
                onSendClicked = viewModel::sendMessage,
                replyingTo = uiState.replyingTo,
                onReplyToCleared = { viewModel.updateReplyingTo(null) }
            )

            AnimatedVisibility(
                visible = uiState.attachment != null && uiState.attachment is Media &&
                        (uiState.attachment?.type == Attachment.Type.Image || uiState.attachment?.type == Attachment.Type.Video),
                enter = scaleIn(),
                exit = scaleOut()
            ) {

                uiState.attachment?.let {
                    PreviewAndSendAttachment(
                        body = uiState.textInput,
                        onBodyChanged = viewModel::updateTextInput,
                        attachment = uiState.attachment as Media,
                        onCloseClicked = { viewModel.updateAttachment(null) },
                        onSendClicked = { viewModel.sendMessage() },
                        replyingTo = uiState.replyingTo,
                        onReplyToCleared = { viewModel.updateReplyingTo(null) }
                    )
                }

            }
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


private fun Context.openDocumentWithChooser(
    document: Media
) {
    val file = document.cacheUri?.let { File(it) }
    val uri = file?.let {
        FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            it
        )
    }
    val openFileIntent = Intent(Intent.ACTION_VIEW)
    openFileIntent.setDataAndType(uri, document.mimeType)
    openFileIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

    val chooserIntent = Intent.createChooser(
        openFileIntent,
        getString(R.string.choose_pdf_title)
    )

    try {
        startActivity(chooserIntent)
    } catch (e: ActivityNotFoundException) {
        e.printStackTrace()
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessagesList(
    messages: LazyPagingItems<MessageItem>,
    messageListState: LazyListState,
    uploadProgressMap: Map<Any, UploadProgress>,
    downloadProgressMap: Map<Any, DownloadProgress>,
    modifier: Modifier = Modifier,
    onResumeAudio: (MessageItem.MessageData, seekValue: Int) -> Unit,
    onPauseAudio: (Media) -> Unit,
    onDocumentClicked: (MessageItem.MessageData) -> Unit,
    onImageClicked: (messageId: String) -> Unit,
    currentlyPlaying: Media?,
    replyTo: (MessageItem.MessageData) -> Unit,
    lastReadMessage: (MessageItem.MessageData) -> Unit
) {

    val scope = rememberCoroutineScope()

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

    val firstMessagesVisible by remember { derivedStateOf { messageListState.firstVisibleItemIndex < 25 } }

    LaunchedEffect(messages.itemCount) {
        if (messages.loadState.prepend is LoadState.NotLoading && !messageListState.isScrollInProgress)
            if (messageListState.firstVisibleItemIndex < 10) {
                messageListState.animateScrollToItem(0)
            }
    }

    var flashColor by remember { mutableStateOf(false) }
//    val infiniteTransition = animateColorAsState(
//        if (flashColor) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color.Transparent,
//        animationSpec = tween(1000, easing = LinearEasing)
//    )

    val infiniteTransition = rememberInfiniteTransition()
    val color by infiniteTransition.animateColor(
        initialValue = MaterialTheme.colorScheme.background,
        targetValue = MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    var flashMessage: Int? by remember { mutableStateOf(null) }

    if (flashColor) {
        LaunchedEffect(Unit) {
            scope.launch {
                delay(2000)
                flashColor = false
            }
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
                items = messages,
                key = { messageItem -> messageItem.itemId ?: messageItem }
            ) { message ->
                when (message) {
                    null -> PlaceHolderMessage()
                    is MessageItem.MessageData -> {
                        val index by remember {
                            derivedStateOf { messages.itemSnapshotList.indexOf(message) }
                        }
                        val nextMessage by remember {
                            derivedStateOf {
                                if (index < messages.itemCount.minus(1)) messages[index + 1]
                                else null
                            }
                        }
                        val previousMessage by remember {
                            derivedStateOf { if (index > 0) messages[index - 1] else null }
                        }

                        SwipeableBox(
                            modifier = Modifier
                                .padding(
                                    top = messageTopPadding(
                                        lastMessage = nextMessage,
                                        currentMessage = message
                                    )
                                )
//                                .background()
                                .animateItemPlacement(),
                            hiddenContent = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_reply),
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .align(Alignment.Center),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    contentDescription = null
                                )
                            },
                            onLeftSwipe = { replyTo(message) },
                            allowedSwipeDirection = SwipeDirection.LEFT,
                            hiddenContentColor = MaterialTheme.colorScheme.surfaceVariant,
                            swipableContentColor = if (flashMessage == index) color else MaterialTheme.colorScheme.background
                        ) {

                            val messageModifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 50.dp)

                            val downloadProgress by remember(downloadProgressMap) {
                                derivedStateOf {
                                    if (message.attachment is Media)
                                        downloadProgressMap[message.id]
                                    else null
                                }
                            }

                            if (message.isFromMe()) {

                                val uploadProgress by remember(uploadProgressMap) {
                                    derivedStateOf {
                                        if (message.attachment is Media)
                                            uploadProgressMap[message.id]
                                        else null
                                    }
                                }

                                SentMessage(
                                    message = message,
                                    uploadProgress = uploadProgress,
                                    downloadProgress = downloadProgress,
                                    nextMessage = nextMessage,
                                    previousMessage = previousMessage,
                                    onResumeAudio = {
                                        onResumeAudio(message, it)
                                    },
                                    onPauseAudio = { onPauseAudio(message.attachment as Media) },
                                    onImageClicked = { onImageClicked(message.id) },
                                    onPdfClicked = { onDocumentClicked(message) },
                                    modifier = messageModifier,
                                    isActiveMessage = currentlyPlaying == message.attachment,
                                    onReplyClicked = {
                                        scope.launch {
                                            val replyMessageIndex = messages
                                                .itemSnapshotList
                                                .indexOfFirst {
                                                    it is MessageItem.MessageData
                                                            && it.id == message.replyTo?.id
                                                }
                                            flashMessage = replyMessageIndex
                                            val isItemVisible = messageListState
                                                .layoutInfo
                                                .visibleItemsInfo
                                                .any { it.index == replyMessageIndex }

                                            if (!isItemVisible) {
                                                messageListState.scrollToItem(replyMessageIndex)
                                            }

                                            flashColor = true
//                                            delay(1000)
//                                            flashColor = false
                                        }
                                    },
                                    contentColor = MaterialTheme.colorScheme.onSecondary,
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            } else {
                                ReceivedMessage(
                                    message = message,
                                    downloadProgress = downloadProgress,
                                    nextMessage = nextMessage,
                                    previousMessage = previousMessage,
                                    onResumeAudio = { onResumeAudio(message, it) },
                                    onPauseAudio = { onPauseAudio(message.attachment as Media) },
                                    onImageClicked = { onImageClicked(message.id) },
                                    onPdfClicked = { onDocumentClicked(message) },
                                    modifier = messageModifier,
                                    isActiveMessage = currentlyPlaying == message.attachment,
                                    onReplyClicked = {
                                        scope.launch {
                                            val replyMessageIndex = messages
                                                .itemSnapshotList
                                                .indexOfFirst {
                                                    it is MessageItem.MessageData
                                                            && it.id == message.replyTo?.id
                                                }
                                            flashMessage = replyMessageIndex
                                            messageListState.scrollToItem(replyMessageIndex)
                                            flashColor = true
//                                            delay(1000)
//                                            flashColor = false
                                        }
                                    },
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                                text = message.date.asSeparator(),
                                color = MaterialTheme.colorScheme.primary,
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
                                    .background(MaterialTheme.colorScheme.onBackground)
                            )
                            Box(
                                modifier = Modifier
                                    .padding(10.dp)
                                    .padding(horizontal = 5.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = message.date.asString(),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(
                                modifier = Modifier
                                    .padding(10.dp)
                                    .height(1.dp)
                                    .weight(1f)
                                    .background(MaterialTheme.colorScheme.onBackground)
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
                onClick = { scope.launch { messageListState.scrollToItem(0) } }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_down),
                    contentDescription = null,
                    tint = Color.Black
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    name: String?,
    status: String?,
    image: ContactImage? = null,
    onBackClicked: () -> Unit,
    onPersonClicked: () -> Unit,
    onMenuClicked: () -> Unit,
    dropDownMenu: @Composable () -> Unit
) {
    CenterAlignedTopAppBar(
        modifier = Modifier
            .fillMaxWidth(),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPersonClicked() }
            ) {


                image?.AsImage(modifier = Modifier.size(40.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 5.dp),
                ) {
                    Text(
                        text = name ?: defaultName,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    AnimatedVisibility(visible = status != null) {
                        status?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClicked, modifier = Modifier.padding(end = 20.dp)) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        actions = {
            IconButton(onClick = onMenuClicked) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_menu),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            dropDownMenu()
        },
        colors = TopAppBarDefaults.largeTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Preview
@Composable
fun PreviewSentMessage() {
    SwipeableBox(
        hiddenContent = {
            Icon(
                painter = painterResource(id = R.drawable.ic_reply),
                modifier = Modifier
                    .padding(10.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                contentDescription = null
            )
        },
        swipableContent = {
            SentMessage(
                message = MessageItem.MessageData(
                    id = "",
                    body = "Hello",
                    date = "12.12.2012",
                    time = "20:00",
                    senderId = "",
                    senderImage = null,
                    senderName = "sender",
                    attachment = null
                ),
                uploadProgress = null,
                downloadProgress = null,
                nextMessage = null,
                previousMessage = null,
                onResumeAudio = { },
                onPauseAudio = { },
                onImageClicked = { },
                onPdfClicked = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .background(MaterialTheme.colorScheme.background),
                isActiveMessage = false,
                onReplyClicked = { },
                contentColor = MaterialTheme.colorScheme.onSecondary,
                containerColor = MaterialTheme.colorScheme.secondary
            )
        },
        onLeftSwipe = { },
        allowedSwipeDirection = SwipeDirection.LEFT,
        hiddenContentColor = MaterialTheme.colorScheme.surfaceVariant,
        swipableContentColor = MaterialTheme.colorScheme.background
    )
}