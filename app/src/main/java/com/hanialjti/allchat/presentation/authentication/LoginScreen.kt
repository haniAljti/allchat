package com.hanialjti.allchat.presentation.authentication

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.hanialjti.allchat.R
import com.hanialjti.allchat.di.getViewModel
import com.hanialjti.allchat.presentation.component.advancedCircleShadow
import com.hanialjti.allchat.presentation.component.advancedRectShadow
import com.hanialjti.allchat.presentation.ui.toConversationsScreen


@Composable
fun LoginScreen(
    navController: NavHostController,
    viewModel: AuthenticationViewModel = getViewModel()
) {

    val uiState by remember(viewModel) { viewModel.uiState }.collectAsState()
    val configuration = LocalConfiguration.current

    LaunchedEffect(uiState.credentialsSaved) {
        if (uiState.credentialsSaved) {
            navController.toConversationsScreen()
        }
    }

    when (configuration.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> {
            Row {
                Box(
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(start = 30.dp)
                            .size(250.dp)
                            .align(Alignment.Center)
                            .shadow(
                                elevation = 75.dp,
                                shape = CircleShape,
                                spotColor = MaterialTheme.colorScheme.primary
                            )
                    )
                }

                LoginForm(
                    username = uiState.username,
                    onUsernameChanged = viewModel::updateUsername,
                    password = uiState.password,
                    onPasswordChanged = viewModel::updatePassword,
                    isPasswordVisible = uiState.isPasswordVisible,
                    onPasswordVisibilityChanged = { viewModel.updateShowPassword((uiState.isPasswordVisible)) },
                    isLoading = uiState.loading,
                    errorMessageRes = uiState.message,
                    onLoginClicked = viewModel::login
                )
            }
        }
        else -> {
            Column {
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Logo(size = 250.dp, modifier = Modifier.align(Alignment.Center))
                }

                LoginForm(
                    username = uiState.username,
                    onUsernameChanged = viewModel::updateUsername,
                    password = uiState.password,
                    onPasswordChanged = viewModel::updatePassword,
                    isPasswordVisible = uiState.isPasswordVisible,
                    onPasswordVisibilityChanged = { viewModel.updateShowPassword((uiState.isPasswordVisible)) },
                    isLoading = uiState.loading,
                    errorMessageRes = uiState.message,
                    onLoginClicked = viewModel::login
                )
            }
        }
    }

//    LazyColumn(
//        Modifier
//            .fillMaxSize()
//            .imePadding()
//            .background(MaterialTheme.colors.background),
//        userScrollEnabled = false
//    ) {
//
//        item {
//            Box(
//                modifier = Modifier.fillMaxWidth()
//            ) {
//                Image(
//                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
//                    contentDescription = null,
//                    modifier = Modifier
//                        .padding(top = 30.dp)
//                        .size(250.dp)
//                        .align(Alignment.Center)
//                        .shadow(
//                            elevation = 75.dp,
//                            shape = CircleShape,
////                            ambientColor = Color(0xFF9EC9C1),
//                            spotColor = Color(0xFF9EC9C1)
//                        )
//                )
//            }
//        }
//        item {
//            Column(
////                Modifier.offset(y = (-150).dp)
//            ) {
//
//                Text(
//                    text = "Sign In",
//                    fontWeight = FontWeight.Bold,
//                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
//                    color = Color.White,
//                    fontSize = 20.sp
//                )
//
//                Spacer(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(horizontal = 20.dp)
//                        .height(1.dp)
//                        .alpha(0.1f)
//                        .background(Color.White)
//                )
//
//                Spacer(modifier = Modifier.height(20.dp))
//                Text(
//                    text = "Username",
//                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp),
//                    color = Color.White
//                )
//                OutlinedTextField(
//                    value = uiState.username,
//                    onValueChange = viewModel::updateUsername,
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(horizontal = 20.dp),
//                    colors = TextFieldDefaults.outlinedTextFieldColors(
//                        textColor = Color.White,
//                        backgroundColor = Color.LightGray.copy(alpha = 0.2f)
//                    ),
//                    singleLine = true,
//                    shape = RoundedCornerShape(10.dp)
//                )
//                Text(
//                    text = "Password",
//                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp),
//                    color = Color.White
//                )
//                OutlinedTextField(
//                    value = uiState.password,
//                    onValueChange = viewModel::updatePassword,
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(horizontal = 20.dp),
//                    colors = TextFieldDefaults.outlinedTextFieldColors(
//                        textColor = Color.White,
//                        backgroundColor = Color.LightGray.copy(alpha = 0.2f)
//                    ),
//                    trailingIcon = {
//                        IconButton(onClick = { viewModel.updateShowPassword(!uiState.isPasswordVisible) }) {
//                            Icon(
//                                painter = painterResource(id = if (uiState.isPasswordVisible) R.drawable.ic_invisible else R.drawable.ic_visible),
//                                contentDescription = null,
//                                tint = Color.White,
//                                modifier = Modifier
//                                    .padding(horizontal = 10.dp)
//                                    .size(30.dp)
//                            )
//                        }
//                    },
//                    visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
//                    singleLine = true,
//                    shape = RoundedCornerShape(10.dp)
//                )
//
//                Box(
//                    modifier = Modifier
//                        .animateContentSize()
//                        .fillMaxWidth()
//                ) {
//                    AnimatedContent(
//                        targetState = uiState.loading,
//                        modifier = Modifier
//                            .align(Alignment.Center)
//                            .height(90.dp)
//                            .padding(20.dp)
//                            .clip(RoundedCornerShape(20.dp))
//                            .background(colorResource(id = R.color.green_light))
//                    ) { isLoading ->
//                        if (isLoading) {
//                            CircularProgressIndicator(
//                                modifier = Modifier
//                                    .padding(5.dp)
//                                    .align(Alignment.Center),
//                                color = MaterialTheme.colors.background
//                            )
//                        } else {
//                            Text(
//                                text = "Sign In",
//                                color = MaterialTheme.colors.background,
//                                modifier = Modifier
//                                    .padding(10.dp)
//                                    .fillMaxSize()
//                                    .align(Alignment.Center)
//                                    .clickable {
//                                        viewModel.login()
//                                    },
//                                textAlign = TextAlign.Center
//                            )
//                        }
//                    }
//
//                }
//
//                AnimatedVisibility(
//                    visible = uiState.message != null,
//                    modifier = Modifier.fillMaxWidth()
//                ) {
//                    Box(
//                        modifier = Modifier
//                            .padding(horizontal = 20.dp)
//                            .fillMaxWidth()
//                            .clip(RoundedCornerShape(20.dp))
//                            .background(Color(0x4deb4034))
//                            .padding(20.dp)
//                    ) {
//                        uiState.message?.let {
//                            Text(
//                                text = stringResource(id = it),
//                                modifier = Modifier.align(
//                                    Alignment.Center
//                                )
//                            )
//                        }
//                    }
//                }
//
//
//                Text(
//                    text = "or sign in with:",
//                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp),
//                    color = Color.White
//                )
//
//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(vertical = 10.dp),
//                    horizontalArrangement = Arrangement.SpaceEvenly
//                ) {
//                    IconButton(
//                        modifier = Modifier
//                            .size(60.dp)
//                            .clip(RoundedCornerShape(20.dp))
//                            .background(colorResource(id = R.color.green_light)),
//                        onClick = { /*TODO*/ }) {
//                        Icon(
//                            painter = painterResource(id = R.drawable.ic_facebook),
//                            tint = Color(0xFF01200C),
//                            contentDescription = null
//                        )
//                    }
//
//                    IconButton(
//                        modifier = Modifier
//                            .size(60.dp)
//                            .clip(RoundedCornerShape(20.dp))
//                            .background(colorResource(id = R.color.green_light)),
//                        onClick = { /*TODO*/ }) {
//                        Icon(
//                            painter = painterResource(id = R.drawable.ic_google),
//                            tint = Color(0xFF01200C),
//                            contentDescription = null
//                        )
//                    }
//
//                    IconButton(
//                        modifier = Modifier
//                            .size(60.dp)
//                            .clip(RoundedCornerShape(20.dp))
//                            .background(colorResource(id = R.color.green_light)),
//                        onClick = { /*TODO*/ }) {
//                        Icon(
//                            painter = painterResource(id = R.drawable.ic_twitter),
//                            tint = Color(0xFF01200C),
//                            contentDescription = null
//                        )
//                    }
//                }
//
//                Text(
//                    text = "New here?",
//                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp),
//                    color = Color.White
//                )
//
//                Chip(
//                    onClick = { /*TODO*/ },
//                    colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF213530)),
//                    modifier = Modifier
//                        .padding(start = 20.dp)
//                        .clip(RoundedCornerShape(50))
//                ) {
//                    Text(text = "Create new account", color = Color.White, fontSize = 12.sp)
//                }
//            }
//
//        }
//    }
}

@Composable
fun Logo(
    modifier: Modifier = Modifier,
    size: Dp = 250.dp
) {
    Image(
        painter = painterResource(id = R.drawable.ic_launcher_foreground),
        contentDescription = null,
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary, BlendMode.Modulate),
        modifier = modifier
            .size(size)
            .advancedCircleShadow(
                size = size,
                shadowBlurRadius = size.times(2),
                radius = size.div(1f).value,
                color = MaterialTheme.colorScheme.primary,
                alpha = 0.3f
            )
    )
}

@Preview
@Composable
fun LogoPreview() {
    Logo()
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun LoginForm(
    username: String,
    onUsernameChanged: (String) -> Unit,
    password: String,
    onPasswordChanged: (String) -> Unit,
    isPasswordVisible: Boolean,
    onPasswordVisibilityChanged: () -> Unit,
    isLoading: Boolean,
    @StringRes errorMessageRes: Int?,
    onLoginClicked: () -> Unit
) {
    Column {

        Text(
            text = stringResource(id = R.string.sign_in),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 20.sp
        )

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(1.dp)
                .alpha(0.1f)
                .background(MaterialTheme.colorScheme.onBackground)
        )

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(id = R.string.username),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp),
            color = Color.White
        )
        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            ),
            singleLine = true,
            shape = RoundedCornerShape(10.dp)
        )
        Text(
            text = stringResource(id = R.string.password),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp),
            color = Color.White
        )
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            ),
            trailingIcon = {
                IconButton(onClick = { onPasswordVisibilityChanged() }) {
                    Icon(
                        painter = painterResource(id = if (isPasswordVisible) R.drawable.ic_invisible else R.drawable.ic_visible),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
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

        Box(
            modifier = Modifier
                .animateContentSize()
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = {
                    if (!isLoading) {
                        onLoginClicked()
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .padding(20.dp)
                    .height(60.dp)
                    .clip(RoundedCornerShape(20.dp)),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                    disabledBackgroundColor = MaterialTheme.colorScheme.secondaryContainer
                ),
            ) {
                AnimatedContent(targetState = isLoading) { isLoading ->

                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    } else {
                        Text(
                            text = stringResource(id = R.string.sign_in),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = errorMessageRes != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(20.dp)
            ) {
                errorMessageRes?.let {
                    Text(
                        text = stringResource(id = it),
                        modifier = Modifier.align(
                            Alignment.Center
                        ),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }


        Text(
            text = stringResource(id = R.string.sign_in_with),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp),
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                onClick = { /*TODO*/ }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_facebook),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    contentDescription = null
                )
            }

            IconButton(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                onClick = { /*TODO*/ }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_google),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    contentDescription = null
                )
            }

            IconButton(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                onClick = { /*TODO*/ }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_twitter),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    contentDescription = null
                )
            }
        }

        Text(
            text = "New here?",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp),
            color = MaterialTheme.colorScheme.onBackground
        )

        Chip(
            onClick = { /*TODO*/ },
            colors = ChipDefaults.chipColors(backgroundColor = MaterialTheme.colorScheme.secondaryContainer),
            modifier = Modifier
                .padding(start = 20.dp)
                .clip(RoundedCornerShape(50))
        ) {
            Text(
                text = "Create new account",
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontSize = 12.sp
            )
        }
    }
}

