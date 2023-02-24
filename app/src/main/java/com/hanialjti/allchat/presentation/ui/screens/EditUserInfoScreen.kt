package com.hanialjti.allchat.presentation.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toFile
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.hanialjti.allchat.R
import com.hanialjti.allchat.common.utils.Logger
import com.hanialjti.allchat.common.utils.scaleCenterCrop
import com.hanialjti.allchat.data.model.Attachment
import com.hanialjti.allchat.data.model.Media
import com.hanialjti.allchat.presentation.component.TopBar
import com.hanialjti.allchat.presentation.viewmodels.EditUserInfoViewModel
import com.hanialjti.allchat.di.getViewModel

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun EditUserInfoScreen(
    navController: NavHostController,
    viewModel: EditUserInfoViewModel = getViewModel()
) {

    val uiState by remember(viewModel) { viewModel.uiState }.collectAsState()
    val filePickerSheetState =
        rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    val context = LocalContext.current

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        bmp?.let {
            val bytes = bmp.scaleCenterCrop(128, 128)
            viewModel.setAvatar(bytes)
        }
    }

    val mediaPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            val bytes = uri?.scaleCenterCrop(context, 1024, 1024)
            viewModel.setAvatar(bytes)
        }

    Column {
        TopBar(
            title = "Edit My Info",
            moreOptions = { },
            onBackClicked = { navController.popBackStack() })

        Column(modifier = Modifier.weight(1f)) {
            Image(
                painter = rememberAsyncImagePainter(
                    uiState.avatar
                ),
                contentDescription = null,
                modifier = Modifier
                    .padding(20.dp)
                    .border(width = 3.dp, color = Color.White, shape = CircleShape)
                    .size(100.dp)
                    .clip(CircleShape)
                    .clickable {
                        mediaPickerLauncher.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    }
            )

            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::setUserName,
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = Color.White,
                ),
                label = {
                    Text("Nickname")
                }
            )

        }

        Button(
            onClick = { viewModel.updateUserInfo() },
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
        ) {
            Text(text = "Save")
        }


    }

}