package com.hanialjti.allchat.presentation.create_chat_room

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanialjti.allchat.data.model.Role
import com.hanialjti.allchat.data.model.UserDetails
import com.hanialjti.allchat.presentation.component.AllChatTextField
import com.hanialjti.allchat.presentation.component.TopBar
import com.hanialjti.allchat.presentation.component.UserItem

@Composable
fun InputRoomInfoScreen(
    title: String,
    onBackPressed: () -> Unit,
    name: String,
    onNameChanged: (String) -> Unit,
//    image: String,
//    onImageChanged: (String) -> Unit,
    invitedUsers: Set<UserDetails>,
    doneButtonText: String,
    isLoading: Boolean,
    onDoneClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        TopBar(
            title = title,
            moreOptions = { },
            onBackClicked = { onBackPressed() }
        )

        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .weight(1f)
        ) {
            Spacer(modifier = Modifier.height(5.dp))

            AllChatTextField(
                fieldTitle = "Room name",
                text = name,
                onTextChanged = onNameChanged
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = "Invitees",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.height(5.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(
                    count = invitedUsers.size,
                    key = { index -> invitedUsers.elementAt(index).id ?: index }
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


        Box(Modifier.fillMaxWidth()) {
            AnimatedLoadingButton(
                text = doneButtonText,
                onClick = onDoneClicked,
                isLoading = isLoading,
                modifier = Modifier.align(Alignment.Center)
            )
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

@Composable
fun AnimatedLoadingButton(
    text: String,
    onClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = isLoading,
        modifier = modifier
            .padding(20.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        if (it) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(10.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Button(
                onClick = onClick,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    text = text,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}

@Preview
@Composable
fun AnimatedLoadingButtonPreview() {
    AnimatedLoadingButton(text = "Done", onClick = { }, isLoading = true)
}

@Preview
@Composable
fun AnimatedNotLoadingButtonPreview() {
    AnimatedLoadingButton(text = "Done", onClick = { }, isLoading = false)
}