package com.hanialjti.allchat.presentation.info

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.hanialjti.allchat.presentation.component.TopBarBackButton
import com.hanialjti.allchat.presentation.component.advancedShadow
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
            ContactImage
                .DefaultProfileImage(false)
                .AsImage(
                    modifier = Modifier
                        .padding(20.dp)
                        .advancedShadow(
                            color = Color(0xFF9EC9C1),
                            shadowBlurRadius = 55.dp,
                            size = 700f,
                            alpha = 0.2f,
                            cornersRadius = 100.dp
                        )
                        .size(70.dp)

                )
        }
    }
}