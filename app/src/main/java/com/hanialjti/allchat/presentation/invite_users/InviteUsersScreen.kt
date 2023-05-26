package com.hanialjti.allchat.presentation.invite_users

import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.hanialjti.allchat.data.model.UserDetails
import com.hanialjti.allchat.di.getViewModel
import com.hanialjti.allchat.presentation.component.AnimatedCheckBox
import com.hanialjti.allchat.presentation.component.TopBar
import org.koin.core.parameter.parametersOf

@Composable
fun InviteUsersScreen(
    conversationId: String,
    navController: NavHostController,
    viewModel: InviteUsersViewModel = getViewModel(parameters = { parametersOf(conversationId) })
) {

    val uiState by remember(viewModel) { viewModel.uiState }.collectAsState()

    SelectableUserList(
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
fun SelectableUserList(
    title: String,
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit,
    allUsers: Set<UserDetails>,
    selectedUsers: Set<UserDetails>,
    onSelectedValueChanged: (UserDetails, Boolean) -> Unit,
    onDoneClicked: () -> Unit,
    doneButtonText: String,
) {
    Box(modifier = modifier) {
        Column {
            TopBar(
                title = title,
                onBackClicked = {
                    onBackPressed()
                }
            ) {
            }

            LazyColumn {
                items(
                    count = allUsers.size,
                    key = { index -> allUsers.elementAt(index).id ?: index }
                ) { index ->
                    val user = allUsers.elementAt(index)

                    SelectableUser(
                        user = user,
                        selected = selectedUsers.contains(user),
                        onSelectedChanged = { onSelectedValueChanged(user, it) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

        }

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .align(Alignment.BottomCenter),
            onClick = onDoneClicked,
            shape = MaterialTheme.shapes.medium
        ) {
            Text(text = doneButtonText, modifier = Modifier.padding(10.dp))
        }
    }

}

@Composable
fun SelectableUser(
    user: UserDetails,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onSelectedChanged: (Boolean) -> Unit
) {
    Row(
        modifier = modifier
            .clickable { onSelectedChanged(!selected) }
            .padding(horizontal = 36.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(modifier = Modifier) {

            user.avatar.AsImage(Modifier.size(60.dp))

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
                        .border(3.dp, MaterialTheme.colorScheme.background, CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
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
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
            }
        }


        AnimatedCheckBox(
            isChecked = selected,
            onClicked = onSelectedChanged,
            modifier = Modifier.size(25.dp)
        )

    }
}

