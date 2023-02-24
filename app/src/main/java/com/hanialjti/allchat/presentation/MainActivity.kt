package com.hanialjti.allchat.presentation

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.rememberBottomSheetNavigator
import com.google.accompanist.permissions.rememberPermissionState
import com.hanialjti.allchat.di.getViewModel
import com.hanialjti.allchat.data.local.datastore.UserCredentials
import com.hanialjti.allchat.data.tasks.ChatForegroundService
import com.hanialjti.allchat.di.get
import com.hanialjti.allchat.presentation.ui.NavigationLayout
import com.hanialjti.allchat.presentation.ui.screens.Screen
import com.hanialjti.allchat.presentation.ui.skin.AllChatChatScreenSkin
import com.hanialjti.allchat.presentation.ui.skin.AllChatConversationScreenSkin
import com.hanialjti.allchat.presentation.ui.skin.DefaultChatScreenSkin
import com.hanialjti.allchat.presentation.ui.skin.DefaultConversationScreenSkin
import com.hanialjti.allchat.presentation.ui.theme.AllChatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AllChat(
                chatSkin = DefaultChatScreenSkin,
                conversationSkin = DefaultConversationScreenSkin,
                lifecycleOwner = lifecycle,
            )
        }
    }
}


@OptIn(ExperimentalMaterialNavigationApi::class)
@Composable
fun AllChat(
    chatSkin: AllChatChatScreenSkin,
    conversationSkin: AllChatConversationScreenSkin,
    lifecycleOwner: Lifecycle,
    userCredentials: UserCredentials? = null
) {

    val bottomSheetNavigator = rememberBottomSheetNavigator()
    val navController = rememberNavController(bottomSheetNavigator)
    val mainViewModel = getViewModel<MainViewModel>()
    val connectionObserver = get<ConnectionLifeCycleObserver>()
    val mainUiState by remember(mainViewModel) { mainViewModel.uiState }.collectAsState()
    val context = LocalContext.current
    val notificationPermissionState = rememberPermissionState(
        permission = Manifest.permission.POST_NOTIFICATIONS
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionState.launchPermissionRequest()
        }
    }

    LaunchedEffect(userCredentials) {
        if (userCredentials != null) {
            mainViewModel.updateUserCredentials(userCredentials)
        }
    }

    LaunchedEffect(Unit) {
        val chatForegroundService = Intent(context, ChatForegroundService::class.java)
        context.startService(chatForegroundService)
    }

    DisposableEffect(
        AllChatTheme {
            val currentNavRoute =
                navController.currentBackStackEntryAsState().value?.destination?.route
            val navItems = listOf(Screen.Conversations, Screen.EditUserInfo)
            Scaffold(
                backgroundColor = MaterialTheme.colors.background,
                bottomBar = {
                    AnimatedVisibility(
                        visible = navItems.any { it.route == currentNavRoute },
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = ExitTransition.None
                    ) {
                        BottomNavigation(
                            backgroundColor = Color.Transparent,
                            elevation = 0.dp,
                            contentColor = Color.Transparent,
                            modifier = Modifier.height(80.dp)
                        ) {
                            navItems.forEach { item ->
                                val isSelected = currentNavRoute == item.route
                                BottomNavigationItem(
                                    selected = isSelected,
                                    onClick = { navController.navigate(item.route, navOptions {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        // Avoid multiple copies of the same destination when
                                        // reselecting the same item
                                        launchSingleTop = true
                                        // Restore state when reselecting a previously selected item
                                        restoreState = true
                                    }) },
                                    icon = {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(60.dp)
                                                .padding(horizontal = 20.dp)
                                                .clip(RoundedCornerShape(25.dp))
                                                .background(if (isSelected) Color(0xFF1F332D) else Color.Transparent)
                                        ) {
                                            Column(
                                                Modifier.align(Alignment.Center),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                if (item.iconRes != null) {
                                                    Icon(
                                                        painter = painterResource(id = item.iconRes),
                                                        contentDescription = null,
                                                        tint = Color.White
                                                    )
                                                }
                                                AnimatedVisibility(visible = isSelected) {
                                                    Text(
                                                        text = item.title,
                                                        color = Color.White,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .padding(paddingValues)
                        .background(MaterialTheme.colors.background)
                ) {
                    NavigationLayout(
                        modifier = Modifier.background(MaterialTheme.colors.background),
                        bottomSheetNavigator = bottomSheetNavigator,
                        navController = navController,
                        loggedInUser = mainUiState.loggedInUser
                    )
                }
            }

        }, lifecycleOwner
    ) {

        lifecycleOwner.addObserver(connectionObserver)

        onDispose {
            lifecycleOwner.removeObserver(connectionObserver)
        }
    }
}


@Composable
fun Lifecycle.observeAsState(): State<Lifecycle.Event> {
    val state = remember { mutableStateOf(Lifecycle.Event.ON_ANY) }
    DisposableEffect(this) {
        val observer = LifecycleEventObserver { _, event ->
            state.value = event
        }
        this@observeAsState.addObserver(observer)
        onDispose {
            this@observeAsState.removeObserver(observer)
        }
    }
    return state
}