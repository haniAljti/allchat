package com.hanialjti.allchat.presentation.chat_entity_details.user_details

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun UserDetailsOption(
    @DrawableRes iconRes: Int,
    text: String,
    switch: @Composable () -> Unit = { }
) {

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier
                .padding(end = 10.dp)
                .size(35.dp),
            tint = MaterialTheme.colorScheme.onBackground
        )

        Box(modifier = Modifier.weight(1f)) {
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        switch()
    }
}

@Composable
fun UserDetailsOptionWithSwitch(
    @DrawableRes iconRes: Int,
    text: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    UserDetailsOption(
        iconRes = iconRes,
        text = text
    ) {
        Switch(checked = isChecked, onCheckedChange = onCheckedChange)
    }
}