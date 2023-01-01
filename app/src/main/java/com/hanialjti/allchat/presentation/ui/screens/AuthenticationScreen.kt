package com.hanialjti.allchat.presentation.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.hanialjti.allchat.R
import com.hanialjti.allchat.di.getViewModel
import com.hanialjti.allchat.presentation.ui.toConversationsScreen
import com.hanialjti.allchat.presentation.viewmodels.AuthenticationViewModel


@Composable
fun AuthenticationScreen(
    navController: NavHostController,
    viewModel: AuthenticationViewModel = getViewModel()
) {

    val uiState by remember(viewModel) { viewModel.uiState }.collectAsState()

    LaunchedEffect(uiState.credentialsSaved) {
        if (uiState.credentialsSaved) {
            navController.toConversationsScreen()
        }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF01200C))) {
        Box(Modifier.fillMaxWidth().weight(0.7f)) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(250.dp).align(Alignment.Center)
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = "Sign In",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                color = Color.White
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(15.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1D8452),
                                Color(0xFF9EC9C1),
                            ),
                            start = Offset(50f, 50f),
                            end = Offset(2000f, 2000f)
                        ),
                        alpha = 0.3f
                    )
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    var isPasswordVisible by remember { mutableStateOf(false) }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Username",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp),
                        color = Color.White
                    )
                    OutlinedTextField(
                        value = uiState.username,
                        onValueChange = viewModel::updateUsername,
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
                    Text(
                        text = "Password",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp),
                        color = Color.White
                    )
                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = viewModel::updatePassword,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = Color.White,
                            backgroundColor = Color.LightGray.copy(alpha = 0.2f)
                        ),
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    painter = painterResource(id = if (isPasswordVisible) R.drawable.ic_invisible else R.drawable.ic_visible),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp).size(30.dp)
                                )
                            }
                        },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Button(
                    onClick = viewModel::login,
                    shape = RoundedCornerShape(15.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .padding(20.dp)
                ) {
                    Text(text = "Sign In")
                }
            }
        }
    }
}