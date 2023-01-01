package com.hanialjti.allchat.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.hanialjti.allchat.R

@Composable
fun TopBarBackButton(
    title: String = "Invite users",
    onBackClicked: () -> Unit
) {
    TopAppBar(
        modifier = Modifier
            .height(80.dp)
            .fillMaxWidth(),
        backgroundColor = Color.Transparent,
        elevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp)
            ) {
                IconButton(
                    onClick = onBackClicked,
                    modifier = Modifier
                        .padding(end = 20.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_back),
                        contentDescription = null,
                        tint = MaterialTheme.colors.primary
                    )
                }

                Text(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 5.dp),
                    text = title,
                    color = MaterialTheme.colors.primary
                )
            }


            Spacer(
                modifier = Modifier
                    .height(1.dp)
                    .fillMaxWidth()
                    .background(MaterialTheme.colors.primary)
            )

        }
    }
}