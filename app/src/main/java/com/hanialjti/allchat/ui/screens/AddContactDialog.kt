package com.hanialjti.allchat.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.hanialjti.allchat.component.BottomSheetKnob
import com.hanialjti.allchat.getViewModel
import com.hanialjti.allchat.viewmodels.AddContactViewModel

@Composable
fun AddContactScreen(
    navController: NavHostController,
    viewModel: AddContactViewModel = getViewModel()
) {

    LaunchedEffect(Unit) { viewModel.updateOwner("user_1") }

    val uiState by remember(viewModel) { viewModel.uiState }.collectAsState()

    Column(verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
        BottomSheetKnob()

        Text(
            text = "Add Contact",
            fontSize = 24.sp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Text(
            text = "Add new contact to your contact list.",
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        OutlinedTextField(
            value = uiState.id,
            onValueChange = viewModel::updateId,
            modifier = Modifier
                .padding(start = 20.dp, end = 20.dp, bottom = 10.dp, top = 20.dp)
                .fillMaxWidth(),
            placeholder = {
                Text(text = "User Id")
            }
        )

        OutlinedTextField(
            value = uiState.nickname,
            onValueChange = viewModel::updateNickName,
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .fillMaxWidth(),
            placeholder = {
                Text(text = "Nickname")
            }
        )

        Button(
            onClick = {
                viewModel.saveContact()
                navController.popBackStack()
            }, modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(text = "Save Contact")
        }
    }
}

