package com.hanialjti.allchat.ui.screens

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
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.BottomEnd
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.hanialjti.allchat.models.Contact
import com.hanialjti.allchat.ui.theme.Green
import com.hanialjti.allchat.ui.toChatScreen
import com.hanialjti.allchat.utils.TWO_DIGIT_FORMAT
import com.hanialjti.allchat.utils.formatTimestamp
import com.hanialjti.allchat.viewmodels.ConversationsViewModel

@Composable
fun ConversationsScreen(
    navController: NavHostController,
    viewModel: ConversationsViewModel = hiltViewModel()
) {
    val conversations = remember(viewModel) { viewModel.conversations("user_1") }
        .collectAsLazyPagingItems()

    Column(horizontalAlignment = CenterHorizontally) {
        Text(
            text = "Conversations",
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 20.sp,
            modifier = Modifier.padding(20.dp)
        )

        ConversationList(
            title = "Conversations",
            conversations = conversations
        ) { conversation ->
            navController.toChatScreen(conversation.id)
        }
    }

}

//@Preview
//@Composable
//fun PreviewConversationList() {
//    listOf(
//        Conversation(
//            id = "",
//            lastMessage = "Hello",
//            isGroupChat = false,
//            name = "Omar Alnaib",
//            imageUrl = "",
//            from = "",
//            unreadMessages = 3,
//            lastUpdated = currentTimestamp
//        ),
//        Conversation(
//            id = "",
//            lastMessage = "Hello",
//            isGroupChat = false,
//            name = "Hani Alalajati",
//            imageUrl = "",
//            from = "",
//            unreadMessages = 100,
//            lastUpdated = currentTimestamp
//        )
//    ).apply {
//        ConversationList(title = "Conversations", conversations = this) { }
//    }
//}

//@Preview
//@Composable
//fun PreviewConversationItem() {
//    Conversation(
//        id = "",
//        lastMessage = "Hello",
//        isGroupChat = false,
//        name = "Omar Alnaib",
//        imageUrl = "",
//        from = "",
//        unreadMessages = 3,
//        lastUpdated = currentTimestamp
//    ).apply {
//        ConversationItem(this) {}
//    }
//
//}

@Composable
fun ConversationList(
    title: String,
    conversations: LazyPagingItems<Contact>,
    onConversationClicked: (Contact) -> Unit
) {



    LazyColumn(
        horizontalAlignment = CenterHorizontally,
    ) {
        items(
            count = conversations.itemCount,
            key = { conversations[it]?.id ?: it }
        ) { index ->

            val conversation = conversations[index]

            conversation?.let {
                ConversationItem(contact = conversation) {
                    onConversationClicked(it)
                }
            } ?: PlaceholderConversation()

        }

    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ConversationItem(contact: Contact, onConversationClicked: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable { onConversationClicked() }
            .padding(15.dp)
            .fillMaxWidth(),
        verticalAlignment = CenterVertically
    ) {

        Box(modifier = Modifier) {

            contact.image?.AsImage(modifier = Modifier.size(70.dp))

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
                        .border(3.dp, Color.White, CircleShape)
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
                    text = it,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            if (contact.composing.isNotEmpty()) {
                //TODO: format composing message
                Text(
                    text = "composing",
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                contact.content?.let {
                    Text(
                        text = it.text.asString(),
                        color = it.color,
                        fontWeight = if (it.bold) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Column(
            horizontalAlignment = CenterHorizontally,
            modifier = Modifier.padding(end = 10.dp)
        ) {
            Text(
                text = contact.lastUpdated.formatTimestamp(TWO_DIGIT_FORMAT),
                color = Color.White,
                modifier = Modifier
            )

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
fun PlaceholderConversation() {
    Row(
        modifier = Modifier
            .padding(15.dp)
            .fillMaxWidth(),
        verticalAlignment = CenterVertically
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

//@Composable
//fun ContactImage(modifier: Modifier = Modifier, painter: Painter, image: String) {
//    Image(
//        painter = painter,
//        contentDescription = null,
//        modifier = modifier
//            .clip(CircleShape),
//        contentScale = ContentScale.Crop
//    )
//}
//
//@Composable
//fun DefaultContactImage(modifier: Modifier = Modifier, isGroupChat: Boolean) {
//    Image(
//        painter = painterResource(id = if (isGroupChat) R.drawable.ic_group else R.drawable.ic_user),
//        contentDescription = null,
//        colorFilter = ColorFilter.tint(Color.White),
//        modifier = modifier
//            .border(width = 3.dp, color = Color.White, shape = CircleShape)
//            .clip(CircleShape)
//    )
//}