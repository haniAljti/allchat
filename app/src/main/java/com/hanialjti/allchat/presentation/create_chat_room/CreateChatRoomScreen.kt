package com.hanialjti.allchat.presentation.create_chat_room

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.hanialjti.allchat.di.getViewModel
import com.hanialjti.allchat.presentation.invite_users.SelectableUsers
import com.hanialjti.allchat.presentation.ui.toChatScreen
import kotlinx.coroutines.launch

@Composable
fun CreateChatRoomScreen(
    navController: NavHostController,
    viewModel: CreateChatRoomViewModel = getViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    if (uiState.isCreated) {
        LaunchedEffect(Unit) {
            uiState.chatId?.let {
                navController.toChatScreen(it, true)
            }
        }
    }

    HorizontalPager(
        pageCount = 2,
        state = uiState.pagerState,
        userScrollEnabled = false
    ) { pageIndex ->
        when (pageIndex) {
            GroupChatCreationStep.SelectInitialParticipants.pageIndex -> {
                SelectableUsers(
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
                    onDoneClicked = {
                        coroutineScope.launch {
                            uiState.pagerState.animateScrollToPage(
                                1
                            )
                        }
                    },
                    doneButtonText = "Next"
                )
            }
            GroupChatCreationStep.InputGroupChatInfo.pageIndex -> {
                InputRoomInfoScreen(
                    title = "Change Room Info",
                    onBackPressed = { navController.popBackStack() },
                    name = uiState.roomName,
                    invitedUsers = uiState.selectedUsers,
                    onNameChanged = viewModel::updateRoomName,
                    doneButtonText = "Done",
                    onDoneClicked = {
                        viewModel.createChatRoom()
                    },
                    modifier = Modifier.fillMaxSize(),
                    isLoading = uiState.isLoading
                )
            }
        }

    }

}
