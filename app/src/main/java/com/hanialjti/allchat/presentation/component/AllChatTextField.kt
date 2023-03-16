package com.hanialjti.allchat.presentation.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AllChatTextField(
    fieldTitle: String,
    text: String,
    onTextChanged: (String) -> Unit
) {

    Text(
        text = fieldTitle,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp),
        color = Color.White
    )

    OutlinedTextField(
        value = text,
        onValueChange = onTextChanged,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            textColor = Color.White,
            backgroundColor = Color.LightGray.copy(alpha = 0.2f)
        ),
        singleLine = true,
        shape = RoundedCornerShape(10.dp)
    )
}