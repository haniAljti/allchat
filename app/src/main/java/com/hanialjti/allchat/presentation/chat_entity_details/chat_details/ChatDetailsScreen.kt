package com.hanialjti.allchat.presentation.chat_entity_details.chat_details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.hanialjti.allchat.common.utils.DATE_TIME_MONTH_SHORT_NO_TIME
import com.hanialjti.allchat.common.utils.asString
import com.hanialjti.allchat.di.getViewModel
import com.hanialjti.allchat.presentation.chat.defaultName
import com.hanialjti.allchat.presentation.component.UserItem
import com.hanialjti.allchat.presentation.chat_entity_details.common.ChatEntityDetailsScreen
import com.hanialjti.allchat.presentation.ui.toUpdateChatDetailsScreen
import com.hanialjti.allchat.presentation.ui.toUserDetailsScreen
import org.koin.core.parameter.parametersOf

@Composable
fun ChatDetailsScreen(
    id: String,
    navController: NavHostController,
    viewModel: ChatDetailsViewModel = getViewModel(parameters = { parametersOf(id) })
) {

    val uiState = viewModel.infoUiState.collectAsState()

    ChatEntityDetailsScreen(
        onBackClicked = { navController.popBackStack() },
        onUpdateChatClicked = { navController.toUpdateChatDetailsScreen(id) },
        isUpdateButtonVisible = true,
        avatar = uiState.value.avatar,
        name = uiState.value.name,
        modifier = Modifier.fillMaxSize()
    ) {

        Column {

            if (!uiState.value.description.isNullOrEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(30))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(15.dp)
                ) {

                    Text(
                        text = "Description",
                        fontSize = 16.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(bottom = 5.dp)
                            .alpha(0.5f)
                    )

                    Text(
                        text = uiState.value.description!!,
                        fontSize = 16.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )

                    uiState.value.createdBy?.let {
                        Spacer(
                            modifier = Modifier
                                .alpha(0.1f)
                                .padding(top = 15.dp)
                                .fillMaxWidth()
                                .background(Color.LightGray)
                                .height(1.dp)
                        )

                        val creationText = buildAnnotatedString {
                            append("Created by ")
                            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                                append(it.nickname)
                            }
                            append(" ")
                            val creationDate = uiState.value.createdAt
                            if (creationDate != null) {
                                append("at ")
                                append(
                                    uiState.value.createdAt!!.asString(DATE_TIME_MONTH_SHORT_NO_TIME)
                                )
//                                withStyle(
//                                    style = SpanStyle(
//                                        fontWeight = FontWeight.Bold,
//                                        color = Color.Red
//                                    )
//                                ) {
//                                    append("W")
//                                }
                            }

                        }
                        Text(
                            text = creationText,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(top = 10.dp)
                        )
                    }
                }
            }


            if (uiState.value.participants.isNotEmpty()) {
                Column(
                    modifier = Modifier.padding(vertical = 20.dp, horizontal = 20.dp)
                ) {

                    Row {
                        Text(
                            text = "${uiState.value.participants.size} Members",
                            fontSize = 16.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(bottom = 5.dp)
                                .alpha(0.5f)
                                .weight(1f)
                        )
                    }

                    LazyColumn {

                        val members = uiState.value.participants

                        item {
                            Button(
                                onClick = { /*TODO*/ },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                shape = RoundedCornerShape(30),
                                modifier = Modifier
                                    .padding(5.dp)
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Text(
                                    text = "Add a new member",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        items(
                            count = members.size,
                            key = { index -> members.elementAt(index).id }
                        ) {

                            val member = members.elementAt(it)

                            UserItem(
                                nickname = member.nickname ?: defaultName,
                                avatar = member.avatar,
                                onUserClicked = { navController.toUserDetailsScreen(member.id) },
                                role = member.role,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }
//        Column {
//
//            TopBar(
//                title = "",
//                onBackClicked = {
//                    navController.popBackStack()
//                },
//                moreOptions = {
//                    Column(
//                        horizontalAlignment = Alignment.CenterHorizontally,
//                        modifier = Modifier.fillMaxWidth()
//                    ) {
//                        Row(verticalAlignment = Alignment.CenterVertically) {
//                            IconButton(
//                                onClick = { navController.toUpdateChatDetailsScreen(id) },
//                                modifier = Modifier
//                                    .padding(end = 20.dp)
//                            ) {
//                                Icon(
//                                    painter = painterResource(id = com.hanialjti.allchat.R.drawable.ic_edit),
//                                    contentDescription = null,
//                                    tint = MaterialTheme.colors.primary
//                                )
//                            }
//                        }
//                    }
//                }
//            )
//
//            Column(
//                horizontalAlignment = Alignment.CenterHorizontally,
//                modifier = Modifier.fillMaxWidth()
//            ) {
//                uiState.value
//                    .avatar
//                    .AsImage(
//                        modifier = Modifier
//                            .padding(bottom = 10.dp, top = 60.dp)
//                            .advancedShadow(
//                                color = Color(0xFF9EC9C1),
//                                shadowBlurRadius = 55.dp,
//                                size = 700f,
//                                alpha = 0.2f,
//                                cornersRadius = 100.dp,
//                                offsetX = (-50).dp,
//                                offsetY = (-50).dp
//                            )
//                            .size(80.dp)
//
//                    )
//
//                uiState.value.name?.let { name ->
//                    Text(
//                        text = name,
//                        fontSize = 16.sp,
//                        color = Color.White,
//                        fontWeight = FontWeight.Bold,
//                        modifier = Modifier.padding(bottom = 20.dp)
//                    )
//                }
//
//                if (!uiState.value.description.isNullOrEmpty()) {
//                    Column(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(horizontal = 20.dp)
//                            .clip(RoundedCornerShape(30))
//                            .background(Color(0xFF3E5A55))
//                            .padding(15.dp)
//                    ) {
//
//                        Text(
//                            text = "Description",
//                            fontSize = 16.sp,
//                            color = Color.White,
//                            fontWeight = FontWeight.Bold,
//                            modifier = Modifier
//                                .padding(bottom = 5.dp)
//                                .alpha(0.5f)
//                        )
//
//                        Text(
//                            text = uiState.value.description!!,
//                            fontSize = 16.sp,
//                            color = Color.White,
//                            fontWeight = FontWeight.Bold,
//                        )
//                    }
//
//                    BasicTextField(
//                        value = desc,
//                        onValueChange = viewModel::updateDescription,
//                        maxLines = 4,
//                        textStyle = TextStyle(
//                            fontSize = 16.sp,
//                            color = Color.White
//                        ),
//                        modifier = Modifier.height(60.dp),
//                        cursorBrush = SolidColor(Color.White),
//                        decorationBox = { innerTextField ->
//                            Row(
//                                Modifier
//                                    .height(45.dp)
//                                    .padding(start = 15.dp)
//                                    .padding(vertical = 1.dp),
//                                verticalAlignment = Alignment.CenterVertically
//                            ) {
//                                Box(Modifier.weight(1f)) {
//                                    if (desc.isEmpty()) androidx.compose.material.Text(
//                                        stringResource(id = R.string.chat_desc_text_field_placeholder),
//                                        style = LocalTextStyle.current.copy(
//                                            color = Color.White,
//                                            fontSize = 16.sp
//                                        )
//                                    )
//                                    innerTextField()
//                                }
//                            }
//                        }
//                    )
//                }
//                }
//
//                if (uiState.value.participants.isNotEmpty()) {
//                    Column(
//                        modifier = Modifier.padding(vertical = 20.dp, horizontal = 20.dp)
//                    ) {
//
//                        Text(
//                            text = "Members",
//                            fontSize = 16.sp,
//                            color = Color.White,
//                            fontWeight = FontWeight.Bold,
//                            modifier = Modifier
//                                .padding(bottom = 5.dp)
//                                .alpha(0.5f)
//                        )
//
//                        uiState.value.participants
//                            .forEach {
//                                it.nickname?.let { it1 ->
//                                    UserItem(
//                                        nickname = it1,
//                                        avatar = it.avatar,
//                                        onUserClicked = { navController.toUserDetailsScreen(it.id) },
//                                        role = it.role,
//                                        modifier = Modifier.padding(vertical = 10.dp)
//                                    )
//                                }
//                            }
//                    }
//                }
//            }

    }
}
