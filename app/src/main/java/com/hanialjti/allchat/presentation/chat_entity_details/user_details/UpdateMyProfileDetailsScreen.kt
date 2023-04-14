package com.hanialjti.allchat.presentation.chat_entity_details.user_details

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.hanialjti.allchat.di.getViewModel
import com.hanialjti.allchat.presentation.component.AllChatTextField
import com.hanialjti.allchat.presentation.conversation.ContactImage
import com.hanialjti.allchat.presentation.chat_entity_details.common.UpdateChatEntityDetailsScreen

@Composable
fun UpdateMyProfileDetailsScreen(
    navController: NavHostController,
    viewModel: UpdateMyProfileInfoViewModel = getViewModel()
) {

    val uiState by remember(viewModel) { viewModel.uiState }.collectAsState()

    UpdateChatEntityDetailsScreen(
        onBackClicked = { navController.popBackStack() },
        onSaveClicked = { viewModel.updateUserInfo() },
        onCancelClicked = { navController.popBackStack() },
        avatar = uiState.avatar,
        onAvatarChosen = { viewModel.setAvatar(ContactImage.DynamicRawImage(it)) }
    ) {
        Column {
            AllChatTextField(
                fieldTitle = "username",
                text = uiState.name,
                onTextChanged = viewModel::setUsername,
                modifier = Modifier.padding(vertical = 10.dp)
            )
            AllChatTextField(
                fieldTitle = "status",
                text = uiState.status,
                onTextChanged = viewModel::setStatus,
                modifier = Modifier.padding(vertical = 10.dp)
            )
        }
    }
//    val filePickerSheetState =
//        rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
//    val context = LocalContext.current
//
//    val cameraLauncher = rememberLauncherForActivityResult(
//        ActivityResultContracts.TakePicturePreview()
//    ) { bmp ->
//        bmp?.let {
//            val bytes = bmp.scaleCenterCrop(128, 128)
//            viewModel.setAvatar(bytes)
//        }
//    }
//
//    val mediaPickerLauncher =
//        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
//            val bytes = uri?.scaleCenterCrop(context, 1024, 1024)
//            viewModel.setAvatar(bytes)
//        }
//
//    Column {
//        TopBar(
//            title = "Edit My Info",
//            moreOptions = { },
//            onBackClicked = { navController.popBackStack() }
//        )
//
//        Column(modifier = Modifier.weight(1f)) {
//            Image(
//                painter = rememberAsyncImagePainter(
//                    uiState.avatar
//                ),
//                contentDescription = null,
//                modifier = Modifier
//                    .padding(20.dp)
//                    .border(width = 3.dp, color = Color.White, shape = CircleShape)
//                    .size(100.dp)
//                    .clip(CircleShape)
//                    .clickable {
//                        mediaPickerLauncher.launch(
//                            PickVisualMediaRequest(
//                                ActivityResultContracts.PickVisualMedia.ImageOnly
//                            )
//                        )
//                    }
//            )
//
//            OutlinedTextField(
//                value = uiState.name,
//                onValueChange = viewModel::setUserName,
//                modifier = Modifier
//                    .padding(20.dp)
//                    .fillMaxWidth(),
//                singleLine = true,
//                colors = TextFieldDefaults.outlinedTextFieldColors(
//                    textColor = Color.White,
//                ),
//                label = {
//                    Text("Nickname")
//                }
//            )
//
//        }
//
//        Button(
//            onClick = { viewModel.updateUserInfo() },
//            modifier = Modifier
//                .padding(20.dp)
//                .fillMaxWidth()
//        ) {
//            Text(text = "Save")
//        }
//
//    }

}