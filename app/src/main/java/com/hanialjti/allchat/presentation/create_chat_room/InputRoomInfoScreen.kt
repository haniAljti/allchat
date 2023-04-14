package com.hanialjti.allchat.presentation.create_chat_room

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanialjti.allchat.R
import com.hanialjti.allchat.data.model.Role
import com.hanialjti.allchat.data.model.User
import com.hanialjti.allchat.presentation.component.AllChatTextField
import com.hanialjti.allchat.presentation.component.TopBar
import com.hanialjti.allchat.presentation.component.UserItem

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun InputRoomInfoScreen(
    title: String,
    onBackPressed: () -> Unit,
    name: String,
    onNameChanged: (String) -> Unit,
//    image: String,
//    onImageChanged: (String) -> Unit,
    invitedUsers: Set<User>,
    doneButtonText: String,
    isLoading: Boolean,
    onDoneClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        TopBar(
            title = title,
            moreOptions = {},
            onBackClicked = { onBackPressed() }
        )

        Column(modifier = Modifier.padding(horizontal = 20.dp).weight(1f)) {
            Spacer(modifier = Modifier.height(5.dp))

            AllChatTextField(
                fieldTitle = "Room name",
                text = name,
                onTextChanged = onNameChanged
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(text = "Invitees", color = Color.White, fontSize = 20.sp)

            Spacer(modifier = Modifier.height(5.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(
                    count = invitedUsers.size,
                    key = { index -> invitedUsers.elementAt(index).id }
                ) { index ->
                    val user = invitedUsers.elementAt(index)

                    user.name?.let {
                        UserItem(
                            nickname = it,
                            avatar = user.avatar,
                            onUserClicked = { },
                            role = Role.Participant,
                            modifier = Modifier
                                .padding(vertical = 10.dp)
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .animateContentSize()
                .fillMaxWidth()
        ) {
            AnimatedContent(
                targetState = isLoading,
                modifier = Modifier
                    .align(Alignment.Center)
                    .height(90.dp)
                    .padding(20.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colorResource(id = R.color.green_light))
            ) { isLoading ->
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(5.dp)
                            .align(Alignment.Center),
                        color = MaterialTheme.colors.background
                    )
                } else {
                    Text(
                        text = doneButtonText,
                        color = MaterialTheme.colors.background,
                        modifier = Modifier
                            .padding(10.dp)
                            .fillMaxSize()
                            .align(Alignment.Center)
                            .clickable {
                                onDoneClicked()
                            },
                        textAlign = TextAlign.Center
                    )
                }
            }

        }
//        Button(
//            modifier = Modifier
//                .padding(20.dp)
//                .fillMaxWidth(),
//            onClick = onDoneClicked,
//            shape = MaterialTheme.shapes.medium
//        ) {
//            Text(text = doneButtonText, modifier = Modifier.padding(10.dp))
//        }
    }
}