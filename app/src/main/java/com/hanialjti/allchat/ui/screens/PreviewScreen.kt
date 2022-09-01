package com.hanialjti.allchat.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.hanialjti.allchat.R
import com.hanialjti.allchat.component.TextInput
import com.hanialjti.allchat.models.UiAttachment
import com.hanialjti.allchat.viewmodels.ChatViewModel

@Composable
fun ImagePreviewScreen(
    messageId: String,
    enableInput: Boolean,
    navController: NavHostController,
    viewModel: ChatViewModel = hiltViewModel()
) {

    val uiState = viewModel.uiState.collectAsState().value
    val attachment by viewModel.getAttachment(messageId).collectAsState(initial = null)

    if (attachment is UiAttachment.Image)
        Box {

            val imageAttachment = attachment as UiAttachment.Image
            val imageSource = imageAttachment.url ?: imageAttachment.cacheUri

            Image(
                painter = rememberAsyncImagePainter(imageSource),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )

            BackArrowTopBar(onBackClicked = { navController.popBackStack() })

            if (enableInput) {
                TextInput(
                    message = uiState.textInput,
                    onMessageChanged = viewModel::updateTextInput,
                    onAttachmentClicked = { },
                    onRecordClicked = { },
                    onRecordLongPressed = { },
                    onRecordReleased = { },
                    onSendClicked = viewModel::sendMessage,
                    attachmentButtonVisible = false,
                    recordButtonVisible = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomEnd)
                )
            }
        }
}

@Composable
fun BackArrowTopBar(
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier.height(80.dp),
        backgroundColor = Color.Transparent,
        elevation = 0.dp
    ) {

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {

            IconButton(onClick = onBackClicked, modifier = Modifier.padding(20.dp)) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back),
                    contentDescription = null,
                    tint = Color.White
                )
            }

            Spacer(
                modifier = Modifier
                    .height(2.dp)
                    .fillMaxWidth()
                    .background(Color.White)
            )
        }

    }
}
