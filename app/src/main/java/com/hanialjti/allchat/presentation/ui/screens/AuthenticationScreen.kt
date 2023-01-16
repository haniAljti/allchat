package com.hanialjti.allchat.presentation.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.hanialjti.allchat.R
import com.hanialjti.allchat.di.getViewModel
import com.hanialjti.allchat.presentation.ui.toConversationsScreen
import com.hanialjti.allchat.presentation.viewmodels.AuthenticationViewModel


@OptIn(ExperimentalMaterialApi::class)
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

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF001708))
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .offset(y = (-50).dp)
                .background(
                    brush = Brush.radialGradient(
                        0f to Color(0x339EC9C1),
                        0.5f to Color.Transparent,
                        1f to Color.Transparent,
                        radius = 1500f
                    )
                )
                .clip(CircleShape)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier
                    .size(250.dp)
                    .align(Alignment.Center)
            )
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .height(550.dp)
        ) {


            Text(
                text = "Sign In",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                color = Color.White,
                fontSize = 20.sp
            )

            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(1.dp)
                    .alpha(0.1f)
                    .background(Color.White)
            )

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
                            modifier = Modifier
                                .padding(horizontal = 10.dp)
                                .size(30.dp)
                        )
                    }
                },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            Button(
                onClick = viewModel::login,
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = colorResource(id = R.color.green_light)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .padding(20.dp)
                    .clip(RoundedCornerShape(20.dp))
            ) {
                Text(text = "Sign In", color = MaterialTheme.colors.background)
            }

            Text(
                text = "or sign in with:",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp),
                color = Color.White
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(colorResource(id = R.color.green_light)),
                    onClick = { /*TODO*/ }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_facebook),
                        tint = Color(0xFF01200C),
                        contentDescription = null
                    )
                }

                IconButton(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(colorResource(id = R.color.green_light)),
                    onClick = { /*TODO*/ }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_google),
                        tint = Color(0xFF01200C),
                        contentDescription = null
                    )
                }

                IconButton(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(colorResource(id = R.color.green_light)),
                    onClick = { /*TODO*/ }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_twitter),
                        tint = Color(0xFF01200C),
                        contentDescription = null
                    )
                }
            }

            Text(
                text = "New here?",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp),
                color = Color.White
            )

            Chip(
                onClick = { /*TODO*/ },
                colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF213530)),
                modifier = Modifier
                    .padding(start = 20.dp)
                    .clip(RoundedCornerShape(50))
            ) {
                Text(text = "Create new account", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}


