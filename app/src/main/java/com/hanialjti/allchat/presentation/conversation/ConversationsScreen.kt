package com.hanialjti.allchat.presentation.conversation

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
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.BottomEnd
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.hanialjti.allchat.common.utils.TWO_DIGIT_FORMAT
import com.hanialjti.allchat.common.utils.asString
import com.hanialjti.allchat.data.model.Contact
import com.hanialjti.allchat.di.getViewModel
import com.hanialjti.allchat.presentation.component.MessageStatusIcon
import com.hanialjti.allchat.presentation.ui.theme.Green
import com.hanialjti.allchat.presentation.ui.toChatScreen
import com.hanialjti.allchat.presentation.ui.toCreateEntityScreen
import kotlinx.datetime.toJavaLocalDateTime
import timber.log.Timber

@Composable
fun ConversationsScreen(
    navController: NavHostController, viewModel: ConversationsViewModel = getViewModel()
) {

    val contacts = remember(viewModel) {
        viewModel.contacts
    }.collectAsLazyPagingItems()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(horizontalAlignment = CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Chats",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.primary,
                fontSize = 20.sp,
                modifier = Modifier
                    .padding(top = 25.dp, bottom = 25.dp)
                    .clickable {
                        viewModel.createChatRoom()
                    })

            Spacer(
                modifier = Modifier
                    .height(2.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .alpha(0.2f)
                    .background(MaterialTheme.colors.primary)
            )

            ConversationList(
                conversations = contacts
            ) { conversation ->
                Timber.d(conversation.id)
                conversation.id?.let {
                    navController.toChatScreen(conversation.id, conversation.isGroupChat)
                }
            }

        }

        FloatingActionButton(
            onClick = { navController.toCreateEntityScreen() },
            modifier = Modifier
                .align(BottomEnd)
                .padding(20.dp)
                .size(70.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null)
        }
    }
}

@Composable
fun ConversationList(
    conversations: LazyPagingItems<Contact>, onConversationClicked: (Contact) -> Unit
) {

    LazyColumn(
        horizontalAlignment = CenterHorizontally, contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        items(
            count = conversations.itemCount
        ) { index ->

            val conversation = conversations[index]

            if (conversation != null) {
                ConversationItem(
                    modifier = Modifier.animateItemPlacement(), contact = conversation
                ) {
                    onConversationClicked(conversation)
                }
            } else PlaceholderConversation()

        }

    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ConversationItem(
    contact: Contact, modifier: Modifier = Modifier, onConversationClicked: () -> Unit
) {
    Row(modifier = modifier
        .clickable { onConversationClicked() }
        .padding(horizontal = 36.dp, vertical = 12.dp)
        .fillMaxWidth(),
        verticalAlignment = CenterVertically) {

        Box(modifier = Modifier) {

            contact.image?.AsImage(modifier = Modifier.size(60.dp))

            androidx.compose.animation.AnimatedVisibility(
                visible = contact.isOnline,
                modifier = Modifier.align(BottomEnd),
                enter = scaleIn(),
                exit = scaleOut()
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .border(3.dp, MaterialTheme.colors.primary, CircleShape)
                        .background(Green)
                )
            }
        }


        Column(
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .weight(1F)
                .padding(start = 10.dp)
        ) {
            contact.name?.let {
                Text(
                    text = it, color = MaterialTheme.colors.primary, fontWeight = FontWeight.Bold
                )
            }

            contact.content?.let {
                Row {
                    ConversationContentText(
                        text = it.text.asString(),
                        color = MaterialTheme.colors.primary,
                        fontWeight = if (it is ContactContent.LastMessage && !it.read) FontWeight.Bold else FontWeight.Normal
                    )
                    Box(modifier = Modifier.height(25.dp)) {
                        if (contact.lastMessage?.isSentByMe == true) {
                            contact.lastMessage.status.let { status ->
                                MessageStatusIcon(messageStatus = status)
                            }
                        }
                    }
                }
            }

        }

        Column(
            horizontalAlignment = CenterHorizontally, modifier = Modifier.padding(end = 10.dp)
        ) {

            contact.lastMessage?.timestamp?.let { lastUpdated ->
                Text(
                    text = lastUpdated.toJavaLocalDateTime().asString(TWO_DIGIT_FORMAT),
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier
                )
            }

            if (contact.unreadMessages > 0) {
                Box(
                    modifier = Modifier
                        .padding(top = 5.dp)
                        .height(25.dp)
                        .defaultMinSize(minWidth = 25.dp)
                        .clip(CircleShape)
                        .background(Green)
                        .align(CenterHorizontally)
                        .padding(PaddingValues(horizontal = 7.dp))
                ) {
                    Text(
                        text = contact.unreadMessages.toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Center)
                    )
                }
            }
        }
    }
}

@Composable
fun ConversationContentText(
    text: String, color: Color, fontWeight: FontWeight, modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = color,
        fontWeight = fontWeight,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
fun PlaceholderConversation() {
    Row(
        modifier = Modifier
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