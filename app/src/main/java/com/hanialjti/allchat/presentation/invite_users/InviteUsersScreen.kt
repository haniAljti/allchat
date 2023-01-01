package com.hanialjti.allchat.presentation.invite_users

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.hanialjti.allchat.R
import com.hanialjti.allchat.data.model.User
import com.hanialjti.allchat.di.getViewModel
import com.hanialjti.allchat.presentation.component.TopBarBackButton
import com.hanialjti.allchat.presentation.ui.theme.Green
import org.koin.core.parameter.parametersOf

@Composable
fun InviteUsersScreen(
    conversationId: String,
    navController: NavHostController,
    viewModel: InviteUsersViewModel = getViewModel(parameters = { parametersOf(conversationId) })
) {

    val uiState by remember(viewModel) { viewModel.uiState }.collectAsState()

    SelectableUsersScreen(
        title = "Select users to invite",
        modifier = Modifier.fillMaxSize(),
        onBackPressed = { navController.popBackStack() },
        allUsers = uiState.allUsers,
        selectedUsers = uiState.selectedUsers,
        onSelectedValueChanged = { user, selected ->
            if (selected) {
                viewModel.addUserToInvitedList(user)
            } else {
                viewModel.removeUserFromInvitedList(user)
            }
        },
        onDoneClicked = { viewModel.inviteSelectedUsers() },
        doneButtonText = "Invite Selected Users"
    )

}

@Composable
fun SelectableUsersScreen(
    title: String,
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit,
    allUsers: List<User>,
    selectedUsers: List<User>,
    onSelectedValueChanged: (User, Boolean) -> Unit,
    onDoneClicked: () -> Unit,
    doneButtonText: String,
) {
    Box(modifier = modifier) {
        Column {
            TopBarBackButton(title) {
                onBackPressed()
            }

            SelectableUsersList(
                allUsers = allUsers,
                selectedUsers = selectedUsers,
                onSelectedValueChanged = onSelectedValueChanged
            )

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

@Composable
fun SelectableUsersList(
    allUsers: List<User>,
    selectedUsers: List<User>,
    onSelectedValueChanged: (User, Boolean) -> Unit
) {

    LazyColumn {
        items(
            count = allUsers.size,
            key = { allUsers[it].id ?: it }
        ) { index ->
            val user = allUsers[index]

            SelectableUser(
                user = user,
                selected = selectedUsers.contains(user),
                onSelectedChanged = { onSelectedValueChanged(user, it) }
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SelectableUser(
    user: User,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onSelectedChanged: (Boolean) -> Unit
) {
    Row(
        modifier = modifier
            .padding(horizontal = 36.dp, vertical = 12.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(modifier = Modifier) {

            UserImage(image = user.image, modifier = Modifier.size(60.dp))
//            user.image?.AsImage(modifier = Modifier.size(60.dp))

            androidx.compose.animation.AnimatedVisibility(
                visible = user.isOnline,
                modifier = Modifier.align(Alignment.BottomEnd),
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
            user.name?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colors.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Box {
            Checkbox(checked = selected, onCheckedChange = onSelectedChanged)
        }

//        Column(
//            horizontalAlignment = Alignment.CenterHorizontally,
//            modifier = Modifier.padding(end = 10.dp)
//        ) {
//
//            contact.lastUpdated?.let { lastUpdated ->
//                Text(
//                    text = lastUpdated.formatTimestamp(TWO_DIGIT_FORMAT),
//                    color = MaterialTheme.colors.primary,
//                    modifier = Modifier
//                )
//            }
//
//            if (contact.unreadMessages > 0) {
//                Box(
//                    modifier = Modifier
//                        .padding(top = 5.dp)
//                        .height(25.dp)
//                        .defaultMinSize(minWidth = 25.dp)
//                        .clip(CircleShape)
//                        .background(Green)
//                        .align(Alignment.CenterHorizontally)
//                        .padding(PaddingValues(horizontal = 7.dp))
//                ) {
//                    Text(
//                        text = contact.unreadMessages.toString(),
//                        color = Color.White,
//                        fontWeight = FontWeight.Bold,
//                        modifier = Modifier.align(Alignment.Center)
//                    )
//                }
//            }
//        }
    }
}

@Composable
fun UserImage(
    image: String?,
    modifier: Modifier = Modifier,
    isGroupChat: Boolean = false
) {
    Image(
        painter = rememberAsyncImagePainter(
            ImageRequest
                .Builder(LocalContext.current)
                .size(50, 50)
                .data(image ?: if (isGroupChat) R.drawable.ic_group else R.drawable.ic_user)
                .build()
        ),
        contentDescription = null,
        colorFilter = if (image == null) ColorFilter.tint(MaterialTheme.colors.primary) else null,
        modifier = modifier
            .clip(CircleShape)
            .apply {
                if (image == null)
                    border(
                        width = 3.dp,
                        color = MaterialTheme.colors.primary,
                        shape = CircleShape
                    )
            },
        contentScale = ContentScale.Crop
    )
}