package com.hanialjti.allchat.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.hanialjti.allchat.ConversationsViewModel
import com.hanialjti.allchat.R
import com.hanialjti.allchat.models.Conversation
import com.hanialjti.allchat.ui.theme.Green
import com.hanialjti.allchat.ui.toChatScreen
import com.hanialjti.allchat.utils.TWO_DIGIT_FORMAT
import com.hanialjti.allchat.utils.currentTimestamp
import com.hanialjti.allchat.utils.formatTimestamp

@Composable
fun ConversationsScreen(
    navController: NavHostController,
    viewModel: ConversationsViewModel = hiltViewModel()
) {

    val conversations: List<Conversation> by remember(viewModel) { viewModel.conversations }
        .collectAsState(initial = listOf())

    ConversationList(
        title = "Conversations",
        conversations = conversations
    ) { conversation ->
        navController.toChatScreen()
    }
}

@Preview
@Composable
fun PreviewConversationList() {
    listOf(
        Conversation(
            id = "",
            lastMessage = "Hello",
            isGroupChat = false,
            name = "Omar Alnaib",
            imageUrl = "",
            ownerId = "",
            members = mutableListOf(),
            unreadMessages = 3,
            lastUpdated = currentTimestamp
        ),
        Conversation(
            id = "",
            lastMessage = "Hello",
            isGroupChat = false,
            name = "Hani Alalajati",
            imageUrl = "",
            ownerId = "",
            members = mutableListOf(),
            unreadMessages = 100,
            lastUpdated = currentTimestamp
        )
    ).apply {
        ConversationList(title = "Conversations", conversations = this) { }
    }
}

@Preview
@Composable
fun PreviewConversationItem() {
    Conversation(
        id = "",
        lastMessage = "Hello",
        isGroupChat = false,
        name = "Omar Alnaib",
        imageUrl = "",
        ownerId = "",
        members = mutableListOf(),
        unreadMessages = 3,
        lastUpdated = currentTimestamp
    ).apply {
        ConversationItem(this) {}
    }

}

@Composable
fun ConversationList(
    title: String,
    conversations: List<Conversation>,
    onConversationClicked: (Conversation) -> Unit
) {
    Column(horizontalAlignment = CenterHorizontally) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 20.sp,
            modifier = Modifier.padding(20.dp)
        )
        conversations.forEach {
            ConversationItem(conversation = it) { onConversationClicked(it) }
        }
    }
}

@Composable
fun ConversationItem(conversation: Conversation, onConversationClicked: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable { onConversationClicked() }
            .padding(15.dp)
            .fillMaxWidth(),
        verticalAlignment = CenterVertically
    ) {

        Image(
            painter = painterResource(id = R.drawable.ic_user),
            contentDescription = null,
            colorFilter = ColorFilter.tint(Color.White),
            modifier = Modifier
                .padding(10.dp)
                .clip(CircleShape)
                .border(width = 3.dp, color = Color.White, shape = CircleShape)
                .size(70.dp)
        )

        Column(
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .weight(1F)
                .padding(start = 10.dp)
        ) {
            conversation.name?.let {
                Text(
                    text = it,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            conversation.lastMessage?.let {
                Text(
                    text = it,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Column(
            horizontalAlignment = CenterHorizontally,
            modifier = Modifier.padding(end = 10.dp)
        ) {
            Text(
                text = conversation.lastUpdated.formatTimestamp(TWO_DIGIT_FORMAT),
                color = Color.White,
                modifier = Modifier
            )

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
                    text = conversation.unreadMessages.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Center)
                )
            }
        }


    }
}