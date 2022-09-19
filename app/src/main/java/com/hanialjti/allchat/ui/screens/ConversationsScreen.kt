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
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.hanialjti.allchat.CustomKoin
import com.hanialjti.allchat.models.Contact
import com.hanialjti.allchat.models.ContactInfo
import com.hanialjti.allchat.ui.theme.Green
import com.hanialjti.allchat.ui.toAddNewContactScreen
import com.hanialjti.allchat.ui.toChatScreen
import com.hanialjti.allchat.ui.toEditUserInfoScreen
import com.hanialjti.allchat.utils.TWO_DIGIT_FORMAT
import com.hanialjti.allchat.utils.formatTimestamp
import com.hanialjti.allchat.viewmodels.ConversationsViewModel
import org.koin.androidx.compose.getViewModel
import timber.log.Timber

@Composable
fun ConversationsScreen(
    navController: NavHostController,
    viewModel: ConversationsViewModel = getViewModel(scope = CustomKoin.getScope())
) {

    val uiState by remember(viewModel) {
        viewModel.uiState
    }.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            Text(
                text = "Chats",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.primary,
                fontSize = 36.sp,
                modifier = Modifier
                    .padding(top = 25.dp, bottom = 25.dp, start = 36.dp)
                    .clickable {
                        navController.toEditUserInfoScreen()
                    }
            )

            Spacer(
                modifier = Modifier
                    .height(2.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .alpha(0.2f)
                    .background(MaterialTheme.colors.primary)
            )

            ConversationList(
                conversations = uiState.contacts
            ) { conversation ->
                Timber.d(conversation.id)
                navController.toChatScreen(conversation.id, conversation.isGroupChat)
            }

        }

        FloatingActionButton(
            onClick = { navController.toAddNewContactScreen() },
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
    conversations: List<Contact>,
    onConversationClicked: (Contact) -> Unit
) {

    LazyColumn(
        horizontalAlignment = CenterHorizontally,
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        items(
            count = conversations.size,
            key = { conversations[it].id }
        ) { index ->

            val conversation = conversations[index]

            ConversationItem(
                modifier = Modifier.animateItemPlacement(),
                contact = conversation
            ) {
                onConversationClicked(conversation)
            }

        }

    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ConversationItem(
    contact: Contact,
    modifier: Modifier = Modifier,
    onConversationClicked: () -> Unit
) {
    Row(
        modifier = modifier
            .clickable { onConversationClicked() }
            .padding(horizontal = 36.dp, vertical = 12.dp)
            .fillMaxWidth(),
        verticalAlignment = CenterVertically
    ) {

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
                    text = it,
                    color = MaterialTheme.colors.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            contact.content?.let {
                ConversationContentText(
                    text = it.text.asString(),
                    color = MaterialTheme.colors.primary,
                    fontWeight = if (it is ContactInfo.LastMessage && !it.read) FontWeight.Bold else FontWeight.Normal
                )
            }

        }

        Column(
            horizontalAlignment = CenterHorizontally,
            modifier = Modifier.padding(end = 10.dp)
        ) {

            contact.lastUpdated?.let { lastUpdated ->
                Text(
                    text = lastUpdated.formatTimestamp(TWO_DIGIT_FORMAT),
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
    modifier: Modifier = Modifier,
    text: String,
    color: Color,
    fontWeight: FontWeight
) {
    Text(
        text = text,
        color = color,
        fontWeight = fontWeight,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
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