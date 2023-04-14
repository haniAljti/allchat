package com.hanialjti.allchat.presentation.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.hanialjti.allchat.presentation.component.BottomSheetKnob
import com.hanialjti.allchat.presentation.ui.toAddNewContactScreen
import com.hanialjti.allchat.presentation.ui.toCreateChatRoomScreens

@Composable
fun CreateEntityScreen(
    navController: NavHostController
) {
    Column {
        BottomSheetKnob()
        EntityObject(
            modifier = Modifier
                .clickable { navController.toAddNewContactScreen() }
                .padding(20.dp)
                .fillMaxWidth(),
            entity = Entity.Contact
        )
        EntityObject(
            modifier = Modifier
                .clickable { navController.toCreateChatRoomScreens() }
                .padding(20.dp)
                .fillMaxWidth(),
            entity = Entity.ChatRoom
        )

        Spacer(modifier = Modifier.padding(10.dp))
    }
}

@Composable
fun EntityObject(modifier: Modifier = Modifier, entity: Entity) {
    Box(modifier = modifier) {
        Text(text = entity.content, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
    }
}

sealed class Entity(val content: String) {
    object Contact : Entity("Add new contact")
    object ChatRoom : Entity("Create chat room")
}