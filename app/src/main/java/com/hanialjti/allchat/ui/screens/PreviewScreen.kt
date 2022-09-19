package com.hanialjti.allchat.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.hanialjti.allchat.CustomKoin
import com.hanialjti.allchat.component.TextInput
import com.hanialjti.allchat.component.TopBar
import com.hanialjti.allchat.models.Attachment
import com.hanialjti.allchat.viewmodels.ChatViewModel
import org.koin.androidx.compose.getViewModel

@Composable
fun ImagePreviewScreen(
    messageId: String,
    enableInput: Boolean,
    navController: NavHostController,
    viewModel: ChatViewModel = getViewModel(scope = CustomKoin.getScope())
) {

    val uiState = viewModel.uiState.collectAsState().value
    val attachment by viewModel.getAttachment(messageId).collectAsState(initial = null)

    if (attachment is Attachment.Image)
        Box {

            val imageAttachment = attachment as Attachment.Image
            val imageSource = imageAttachment.url ?: imageAttachment.cacheUri

            Image(
                painter = rememberAsyncImagePainter(imageSource),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )

            TopBar(onBackClicked = { navController.popBackStack() })

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
