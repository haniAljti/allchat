package com.hanialjti.allchat.presentation.preview_attachment

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import coil.compose.rememberAsyncImagePainter
import com.hanialjti.allchat.presentation.chat.Attachment
import com.hanialjti.allchat.presentation.component.TopBar

@Composable
fun ImagePreview(
    modifier: Modifier,
    onBackClicked: () -> Unit,
    attachment: Attachment,
) {

    Box(modifier) {
        if (attachment is Attachment.Image) {

            val imageSource by remember(attachment) {
                mutableStateOf(attachment.url ?: attachment.cacheUri)
            }

            Image(
                painter = rememberAsyncImagePainter(imageSource),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )

            TopBar(onBackClicked = onBackClicked)

//            if (enableInput) {
//                TextInput(
//                    message = uiState.textInput,
//                    onMessageChanged = viewModel::updateTextInput,
//                    onAttachmentClicked = { },
//                    onRecordClicked = { },
//                    onRecordLongPressed = { },
//                    onRecordReleased = { },
//                    onSendClicked = viewModel::sendMessage,
//                    attachmentButtonVisible = false,
//                    recordButtonVisible = false,
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .align(Alignment.BottomEnd)
//                )
//            }
        }
    }
}
