package com.hanialjti.allchat.presentation.info

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.hanialjti.allchat.presentation.component.TopBarBackButton
import com.hanialjti.allchat.presentation.conversation.ContactImage

@Composable
fun InfoScreen(
    id: String,
    navController: NavHostController
) {
    Column {

        TopBarBackButton("Profile") {
            navController.popBackStack()
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            ContactImage.DefaultProfileImage(false).AsImage(modifier = Modifier.size(50.dp))
        }
    }
}