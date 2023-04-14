package com.hanialjti.allchat.presentation.preview_attachment

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.hanialjti.allchat.data.model.Media
import com.hanialjti.allchat.data.model.MessageItem
import com.hanialjti.allchat.presentation.component.TextInput
import com.hanialjti.allchat.presentation.component.TopBar

@Composable
fun PreviewAndSendAttachment(
    body: String,
    replyingTo: MessageItem.MessageData?,
    onBodyChanged: (String) -> Unit,
    attachment: Media,
    onCloseClicked: () -> Unit,
    onSendClicked: () -> Unit,
    onReplyToCleared: () -> Unit
) {

    Column(Modifier.background(androidx.compose.material.MaterialTheme.colors.background)) {


        TopBar(
            title = "",
            onBackClicked = onCloseClicked,
            modifier = Modifier.height(75.dp),
            moreOptions = {}
        )


        Image(
            painter = rememberAsyncImagePainter(
                attachment.cacheUri ?: attachment.url,
                contentScale = ContentScale.FillWidth
            ),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        TextInput(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111A14)),
            message = body,
            attachmentButtonVisible = false,
            recordButtonVisible = false,
            onMessageChanged = onBodyChanged,
            onOpenGallery = { },
            onOpenCamera = { },
            onSelectDocument = { },
            onRecordClicked = { },
            onRecordingCancelled = { },
            onRecordingStarted = { },
            onRecordingEnded = { },
            onSendClicked = onSendClicked,
            replyingTo = replyingTo,
            onReplyToCleared = onReplyToCleared
        )

    }

}