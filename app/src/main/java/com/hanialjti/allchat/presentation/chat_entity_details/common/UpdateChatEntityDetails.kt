package com.hanialjti.allchat.presentation.chat_entity_details.common

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hanialjti.allchat.common.utils.scaleCenterCrop
import com.hanialjti.allchat.presentation.component.TopBar
import com.hanialjti.allchat.presentation.component.advancedRectShadow
import com.hanialjti.allchat.presentation.conversation.ContactImage

@Composable
fun UpdateChatEntityDetailsScreen(
    errorText: String? = null,
    onBackClicked: () -> Unit,
    onSaveClicked: () -> Unit,
    onCancelClicked: () -> Unit,
    avatar: ContactImage,
    onAvatarChosen: (ByteArray) -> Unit,
    content: @Composable () -> Unit
) {

    val context = LocalContext.current

    val mediaPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            val bytes = uri?.scaleCenterCrop(context, 1024, 1024)
            if (bytes != null) {
                onAvatarChosen(bytes)
            }
        }

    Column(modifier = Modifier) {
        TopBar(
            title = "",
            onBackClicked = onBackClicked,
            moreOptions = {}
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .weight(1f)
        ) {
            avatar
                .AsImage(
                    modifier = Modifier
                        .padding(bottom = 10.dp, top = 60.dp)
                        .advancedRectShadow(
                            color = MaterialTheme.colorScheme.primary,
                            shadowBlurRadius = 55.dp,
                            size = 700f,
                            alpha = 0.2f,
                            cornersRadius = 100.dp,
                            offsetX = (-50).dp,
                            offsetY = (-50).dp
                        )
                        .clickable {
                            mediaPickerLauncher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        }
                        .size(80.dp)
                )

            Box(modifier = Modifier.weight(1f)) {
                content()
            }

            val errorTextCopy by remember { mutableStateOf(errorText) }

            AnimatedVisibility(visible = errorText != null) {

                errorTextCopy?.let {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20))
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Error",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(text = it, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }


            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.End)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCancelClicked,
                    shape = RoundedCornerShape(30),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Cancel", color = MaterialTheme.colorScheme.onBackground)
                }
                FilledTonalButton(
                    onClick = onSaveClicked,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(30),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Save", color = MaterialTheme.colorScheme.onPrimary)
                }
            }

        }
    }
}