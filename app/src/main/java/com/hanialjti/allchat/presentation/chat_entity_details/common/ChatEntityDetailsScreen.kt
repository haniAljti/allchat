package com.hanialjti.allchat.presentation.chat_entity_details.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanialjti.allchat.R
import com.hanialjti.allchat.presentation.component.TopBar
import com.hanialjti.allchat.presentation.component.advancedRectShadow
import com.hanialjti.allchat.presentation.conversation.ContactImage

@Composable
fun ChatEntityDetailsScreen(
    onBackClicked: () -> Unit,
    onUpdateChatClicked: () -> Unit,
    isUpdateButtonVisible: Boolean,
    avatar: ContactImage,
    name: String?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
    ) {
    Column(modifier = modifier) {
        TopBar(
            title = "",
            onBackClicked = onBackClicked,
            moreOptions = {
                if (isUpdateButtonVisible) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onUpdateChatClicked,
                            modifier = Modifier
                                .padding(end = 20.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_edit),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            avatar.AsImage(
                modifier = Modifier
                    .padding(bottom = 10.dp, top = 60.dp)
                    .advancedRectShadow(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shadowBlurRadius = 55.dp,
                        size = 700f,
                        alpha = 0.2f,
                        cornersRadius = 100.dp,
                        offsetX = (-50).dp,
                        offsetY = (-50).dp
                    )
                    .size(80.dp)

            )

            name?.let {
                Text(
                    text = name,
                    fontSize = 16.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            }

            content()

        }
    }
}