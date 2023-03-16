package com.hanialjti.allchat.presentation.create_chat_room

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hanialjti.allchat.data.model.User
import com.hanialjti.allchat.presentation.component.AllChatTextField
import com.hanialjti.allchat.presentation.component.TopBarBackButton

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
    onDoneClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Column {
            TopBarBackButton(title) {
                onBackPressed()
            }

            Spacer(modifier = Modifier.height(5.dp))

            AllChatTextField(fieldTitle = "Room name", text = name, onTextChanged = onNameChanged)

            Spacer(modifier = Modifier.height(5.dp))

            Text(text = "Invitees", color = Color.White)

            Spacer(modifier = Modifier.height(5.dp))

            LazyVerticalGrid(columns = GridCells.Adaptive(50.dp)) {
                items(
                    count = invitedUsers.size,
                    key = { index -> invitedUsers.elementAt(index).id }
                ) { index ->
                    val user = invitedUsers.elementAt(index)

                    Column {
                        user.avatar?.AsImage()
                        user.name?.let { Text(text = it) }
                    }
                }
            }
        }

        Button(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            onClick = onDoneClicked,
            shape = MaterialTheme.shapes.medium
        ) {
            Text(text = doneButtonText, modifier = Modifier.padding(10.dp))
        }
    }
}