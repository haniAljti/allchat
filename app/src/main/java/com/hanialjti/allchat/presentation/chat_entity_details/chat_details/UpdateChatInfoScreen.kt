package com.hanialjti.allchat.presentation.chat_entity_details.chat_details

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.hanialjti.allchat.di.getViewModel
import com.hanialjti.allchat.presentation.component.AllChatTextField
import com.hanialjti.allchat.presentation.chat_entity_details.common.UpdateChatEntityDetailsScreen
import org.koin.core.parameter.parametersOf


@Composable
fun UpdateChatInfoScreen(
    chatId: String,
    navController: NavHostController,
    viewModel: UpdateChatDetailsViewModel = getViewModel(parameters = { parametersOf(chatId) })
) {

    val uiState = viewModel.uiState.collectAsState()

    UpdateChatEntityDetailsScreen(
        errorText = uiState.value.errorMessage,
        onBackClicked = { navController.popBackStack() },
        onSaveClicked = { viewModel.saveChanges() },
        onCancelClicked = { navController.popBackStack() },
        avatar = uiState.value.avatar,
        onAvatarChosen = { viewModel.updateAvatar(it) }
    ) {
        Column {
            AllChatTextField(
                fieldTitle = "Room name",
                text = uiState.value.name,
                onTextChanged = viewModel::updateName,
                modifier = Modifier.padding(vertical = 10.dp)
            )
            AllChatTextField(
                fieldTitle = "Description",
                text = uiState.value.description,
                onTextChanged = viewModel::updateDescription,
                modifier = Modifier.padding(vertical = 10.dp)
            )
        }
    }
}
