package com.hanialjti.allchat.presentation.create_chat_room

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.hanialjti.allchat.di.getViewModel
import com.hanialjti.allchat.presentation.invite_users.SelectableUsersScreen

@Composable
fun SelectInitialParticipantsScreen(
    navController: NavHostController,
    viewModel: CreateChatRoomViewModel = getViewModel()
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
        onDoneClicked = {  },
        doneButtonText = "Invite Selected Users"
    )
}