package com.hanialjti.allchat.presentation.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanialjti.allchat.R

@Composable
fun TopBar(
    modifier: Modifier = Modifier,
    title: String? = "",
    onBackClicked: () -> Unit,
    moreOptions: @Composable () -> Unit
) {
    TopAppBar(
        modifier = modifier.height(80.dp),
        backgroundColor = Color.Transparent,
        elevation = 0.dp
    ) {

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClicked, modifier = Modifier.padding(20.dp)) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_back),
                        contentDescription = null,
                        tint = Color.White
                    )
                }

                title?.let {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = modifier
                            .weight(1f)
                    )
                }

                moreOptions()
            }

//            Spacer(
//                modifier = Modifier
//                    .height(2.dp)
//                    .fillMaxWidth()
//                    .background(Color.White)
//            )
        }
    }
}