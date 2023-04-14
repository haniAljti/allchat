@file:JvmName("TopBarKt")

package com.hanialjti.allchat.presentation.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.*

//@Composable
//fun TopBar(
//    title: String = "Invite users",
//    onBackClicked: () -> Unit
//) {
//    TopAppBar(
//        modifier = Modifier
//            .height(80.dp)
//            .fillMaxWidth(),
//        backgroundColor = Color.Transparent,
//        elevation = 0.dp
//    ) {
//        Column(modifier = Modifier.fillMaxWidth()) {
//
//            Row(
//                verticalAlignment = Alignment.CenterVertically,
//                modifier = Modifier
//                    .weight(1f)
//                    .clip(RoundedCornerShape(bottomEnd = 20.dp, bottomStart = 20.dp))
//                    .background(Color(0xFF3E5A55))
//                    .padding(horizontal = 20.dp)
//            ) {
//                IconButton(
//                    onClick = onBackClicked,
//                    modifier = Modifier
//                        .padding(end = 20.dp)
//                ) {
//                    Icon(
//                        painter = painterResource(id = R.drawable.ic_back),
//                        contentDescription = null,
//                        tint = MaterialTheme.colors.primary
//                    )
//                }
//
//                Text(
//                    modifier = Modifier
//                        .weight(1f)
//                        .padding(start = 5.dp),
//                    text = title,
//                    color = MaterialTheme.colors.primary
//                )
//            }
//
//
////            Spacer(
////                modifier = Modifier
////                    .height(1.dp)
////                    .fillMaxWidth()
////                    .background(MaterialTheme.colors.primary)
////            )
//
//        }
//    }
//}