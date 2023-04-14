package com.hanialjti.allchat.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanialjti.allchat.data.model.Role
import com.hanialjti.allchat.presentation.conversation.ContactImage

@Composable
fun UserItem(
    nickname: String,
    avatar: ContactImage,
    onUserClicked: () -> Unit,
    role: Role,
    modifier: Modifier = Modifier
) {

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable { onUserClicked() }
            .fillMaxWidth()
    ) {

        avatar.AsImage(modifier = Modifier.size(50.dp))

        Text(
            text = nickname,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f).padding(horizontal = 20.dp)
        )

        if (role != Role.Participant) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(30))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = role.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

    }


}