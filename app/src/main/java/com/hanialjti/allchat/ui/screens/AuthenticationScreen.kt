package com.hanialjti.allchat.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import com.hanialjti.allchat.getViewModel
import com.hanialjti.allchat.ui.toConversationsScreen
import com.hanialjti.allchat.viewmodels.AuthenticationViewModel


@Composable
fun AuthenticationScreen(
    navController: NavHostController,
    viewModel: AuthenticationViewModel = getViewModel()
) {

    val uiState by remember(viewModel) { viewModel.uiState }.collectAsState()

    LaunchedEffect(uiState.credentialsSaved) {
        if (uiState.credentialsSaved) {
            navController.toConversationsScreen()
        }
    }

    Column {
        OutlinedTextField(value = uiState.username, onValueChange = viewModel::updateUsername)
        OutlinedTextField(value = uiState.password, onValueChange = viewModel::updatePassword)
        Button(onClick = viewModel::login) {
            Text(text = "Login")
        }
    }
}