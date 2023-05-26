package com.hanialjti.allchat.presentation.conversation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.BottomEnd
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import com.hanialjti.allchat.R
import com.hanialjti.allchat.common.utils.TWO_DIGIT_FORMAT
import com.hanialjti.allchat.common.utils.asString
import com.hanialjti.allchat.data.model.ContactWithLastMessage
import com.hanialjti.allchat.di.getViewModel
import com.hanialjti.allchat.presentation.component.MessageStatusIcon
import com.hanialjti.allchat.presentation.ui.*
import com.hanialjti.allchat.presentation.ui.screens.Entity
import com.hanialjti.allchat.presentation.ui.screens.EntityObject
import kotlinx.datetime.toJavaLocalDateTime
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    navController: NavHostController,
    viewModel: ConversationsViewModel = getViewModel()
) {

    val contacts = viewModel.contacts.collectAsLazyPagingItems()
    val uiState = viewModel.conversationsUiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.synchronize()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {

            val topBarState = rememberTopAppBarState()
            val scrollBehavior =
                TopAppBarDefaults.exitUntilCollapsedScrollBehavior(state = topBarState)
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    LargeTopAppBar(
                        title = {
                            Text(
                                text = "Chats",
                                fontWeight = FontWeight.Bold,
                                fontSize = 30.sp,
                            )
                        },
                        actions = {
                            Row {
                                IconButton(
                                    onClick = { navController.toEditUserInfoScreen() }
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_user_edit),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.largeTopAppBarColors(
                            titleContentColor = MaterialTheme.colorScheme.onBackground,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        scrollBehavior = scrollBehavior

                    )
                },
                content = { innerPadding ->
                    AnimatedVisibility(visible = uiState.value.isSynchronizing) {
                        Row(verticalAlignment = CenterVertically) {
                            Text(text = "Synchronizing...")
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        }
                    }

                    ConversationList(
                        conversations = contacts,
                        modifier = Modifier.padding(innerPadding)
                    ) { conversation ->
                        Timber.d(conversation.id)
                        conversation.id?.let {
                            navController.toChatScreen(conversation.id, conversation.isGroupChat)
                        }
                    }
                }
            )

//            Row(
//                verticalAlignment = CenterVertically,
//                modifier = Modifier.padding(horizontal = 25.dp, vertical = 15.dp)
//            ) {
//                Text(
//                    text = "Chats",
//                    fontWeight = FontWeight.Bold,
//                    color = MaterialTheme.colorScheme.onBackground,
//                    fontSize = 20.sp,
//                    modifier = Modifier.weight(1f)
//                )
//
//                IconButton(
//                    onClick = { navController.toEditUserInfoScreen() }
//                ) {
//                    androidx.compose.material3.Icon(
//                        painter = painterResource(id = R.drawable.ic_user_edit),
//                        contentDescription = null,
//                        tint = MaterialTheme.colorScheme.onBackground
//                    )
//                }
//
//            }


//            Spacer(
//                modifier = Modifier
//                    .height(2.dp)
//                    .fillMaxWidth()
//                    .padding(horizontal = 20.dp)
//                    .alpha(0.2f)
//                    .background(MaterialTheme.colorScheme.onBackground)
//            )


        }

        FloatingActionButton(
            onClick = { viewModel.updateIsCreateChatMenuOpen(true) },
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(BottomEnd)
                .padding(20.dp)
        ) {
//            with(context) {
//                contentResolver.openFileDescriptor(uri, "r")?.use { parcelFileDescriptor ->
//                    val pdfRenderer = PdfRenderer(parcelFileDescriptor).openPage(0)
//                    val bitmap = Bitmap.createBitmap(pdfRenderer.width, pdfRenderer.height, Bitmap.Config.ARGB_8888)
//                    pdfRenderer.render(bitmap, null, null, pdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
//                    pdfRenderer.close()
//                }
//            }
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }

        if (uiState.value.isCreateChatMenuOpen)
            ModalBottomSheet(onDismissRequest = { viewModel.updateIsCreateChatMenuOpen(false) }) {
                Column {
                    EntityObject(
                        modifier = Modifier
                            .clickable {
                                viewModel.updateIsCreateChatMenuOpen(false)
                                navController.toAddNewContactScreen()
                            }
                            .padding(20.dp)
                            .fillMaxWidth(),
                        entity = Entity.Contact
                    )
                    EntityObject(
                        modifier = Modifier
                            .clickable {
                                viewModel.updateIsCreateChatMenuOpen(false)
                                navController.toCreateChatRoomScreens()
                            }
                            .padding(20.dp)
                            .fillMaxWidth(),
                        entity = Entity.ChatRoom
                    )
                }
            }
    }
}

@Composable
fun ConversationList(
    conversations: LazyPagingItems<ContactWithLastMessage>,
    modifier: Modifier = Modifier,
    onConversationClicked: (ContactWithLastMessage) -> Unit,
) {

    LazyColumn(
        horizontalAlignment = CenterHorizontally,
        contentPadding = PaddingValues(bottom = 80.dp),
        modifier = modifier
    ) {
        items(
            items = conversations,
            key = { it.id ?: it }
        ) { conversation ->

            if (conversation != null) {
                ConversationItem(
                    contactWithLastMessage = conversation,
                    modifier = Modifier.animateItemPlacement(),
                ) {
                    onConversationClicked(conversation)
                }
            } else PlaceholderConversation(modifier = Modifier.animateItemPlacement())

        }

    }
}

@Composable
fun ConversationItem(
    contactWithLastMessage: ContactWithLastMessage,
    modifier: Modifier = Modifier,
    onConversationClicked: () -> Unit
) {
    Row(modifier = modifier
        .clickable { onConversationClicked() }
        .padding(horizontal = 36.dp, vertical = 12.dp)
        .fillMaxWidth(),
        verticalAlignment = CenterVertically) {

        Box(modifier = Modifier) {

            contactWithLastMessage.image?.AsImage(modifier = Modifier.size(60.dp))

            androidx.compose.animation.AnimatedVisibility(
                visible = contactWithLastMessage.isOnline,
                modifier = Modifier.align(BottomEnd),
                enter = scaleIn(),
                exit = scaleOut()
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
//                        .shadow(elevation = 50.dp, shape = CircleShape)
                        .border(3.dp, MaterialTheme.colorScheme.background, CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary)
                )
            }
        }

        Column(Modifier.padding(horizontal = 10.dp)) {

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {

                contactWithLastMessage.name?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                } ?: Spacer(modifier = Modifier.width(1.dp))

                contactWithLastMessage.lastMessage?.timestamp?.let { lastUpdated ->
                    Text(
                        text = lastUpdated.toJavaLocalDateTime().asString(TWO_DIGIT_FORMAT),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.align(Alignment.Bottom)
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {

                contactWithLastMessage.content?.let {
                    Row {
                        ConversationContentText(
                            text = it.text.asString(),
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = if (it is ContactContent.LastMessage && !it.read) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        Box(modifier = Modifier.height(25.dp)) {
                            if (contactWithLastMessage.lastMessage?.isSentByMe == true) {
                                contactWithLastMessage.lastMessage.status.let { status ->
                                    MessageStatusIcon(messageStatus = status)
                                }
                            }
                        }
                    }
                }

                if (contactWithLastMessage.unreadMessages > 0) {
                    Box(
                        modifier = Modifier
                            .padding(top = 5.dp)
                            .height(25.dp)
                            .defaultMinSize(minWidth = 25.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(PaddingValues(horizontal = 7.dp))
                    ) {
                        Text(
                            text = contactWithLastMessage.unreadMessages.toString(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Center)
                        )
                    }
                }

            }

        }

    }
}

@Composable
fun ConversationContentText(
    text: String,
    color: Color,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = color,
        fontWeight = fontWeight,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
fun PlaceholderConversation(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(15.dp)
            .fillMaxWidth(), verticalAlignment = CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(70.dp)
                .background(color = Color.LightGray, shape = CircleShape)

        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(60.dp)
                .padding(15.dp)
                .background(color = Color.LightGray, shape = RoundedCornerShape(10.dp))
        )

    }
}