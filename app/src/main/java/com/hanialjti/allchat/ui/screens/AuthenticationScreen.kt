package com.hanialjti.allchat.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import com.hanialjti.allchat.CustomKoin
import com.hanialjti.allchat.ui.toConversationsScreen
import com.hanialjti.allchat.viewmodels.AuthenticationViewModel
import org.koin.androidx.compose.getViewModel

@Composable
fun AuthenticationScreen(
    navController: NavHostController,
    viewModel: AuthenticationViewModel = getViewModel(scope = CustomKoin.getScope())
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