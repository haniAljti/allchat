package com.hanialjti.allchat.presentation.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hanialjti.allchat.presentation.component.BottomSheetKnob

@Composable
fun CreateEntityScreen(

) {
    Column {
        BottomSheetKnob()
        EntityObject(modifier = Modifier.padding(20.dp).clickable {  }, entity = Entity.Contact)
        EntityObject(modifier = Modifier.padding(20.dp), entity = Entity.ChatRoom)
    }
}

@Composable
fun EntityObject(modifier: Modifier = Modifier, entity: Entity) {
    Text(text = entity.content, modifier)
}

sealed class Entity(val content: String) {
    object Contact : Entity("Add new contact")
    object ChatRoom : Entity("Create chat room")
}