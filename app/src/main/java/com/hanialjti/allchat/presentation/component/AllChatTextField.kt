package com.hanialjti.allchat.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AllChatTextField(
    fieldTitle: String,
    text: String,
    onTextChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {

    Column(modifier) {

        Text(
            text = fieldTitle,
            modifier = Modifier.padding(vertical = 5.dp),
            color = Color.White
        )

        OutlinedTextField(
            value = text,
            onValueChange = onTextChanged,
            modifier = Modifier
                .fillMaxWidth(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = MaterialTheme.colorScheme.onSecondaryContainer,
                backgroundColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            singleLine = true,
            shape = RoundedCornerShape(10.dp)
        )
    }
}
