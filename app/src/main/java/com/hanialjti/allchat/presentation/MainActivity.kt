package com.hanialjti.allchat.presentation

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.rememberBottomSheetNavigator
import com.google.accompanist.permissions.rememberPermissionState
import com.hanialjti.allchat.data.local.datastore.UserCredentials
import com.hanialjti.allchat.di.get
import com.hanialjti.allchat.di.getViewModel
import com.hanialjti.allchat.presentation.ui.NavigationLayout
import com.hanialjti.allchat.presentation.ui.skin.AllChatChatScreenSkin
import com.hanialjti.allchat.presentation.ui.skin.AllChatConversationScreenSkin
import com.hanialjti.allchat.presentation.ui.skin.DefaultChatScreenSkin
import com.hanialjti.allchat.presentation.ui.skin.DefaultConversationScreenSkin
import com.hanialjti.allchat.presentation.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            AllChat(
                chatSkin = DefaultChatScreenSkin,
                conversationSkin = DefaultConversationScreenSkin,
                lifecycleOwner = lifecycle,
            )
        }
    }
}


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterialNavigationApi::class)
@Composable
fun AllChat(
    chatSkin: AllChatChatScreenSkin,
    conversationSkin: AllChatConversationScreenSkin,
    lifecycleOwner: Lifecycle,
    userCredentials: UserCredentials? =
        UserCredentials("hani", "15960400")
) {

    val bottomSheetNavigator = rememberBottomSheetNavigator()
    val navController = rememberNavController(bottomSheetNavigator)
    val mainViewModel = getViewModel<MainViewModel>()
    val connectionObserver = get<ConnectionLifeCycleObserver>()
    val mainUiState by remember(mainViewModel) { mainViewModel.uiState }.collectAsState()

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

    DisposableEffect(
        AppTheme {
            Box(
                modifier = Modifier
                    .systemBarsPadding()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                NavigationLayout(
                    modifier = Modifier.fillMaxSize(),
                    bottomSheetNavigator = bottomSheetNavigator,
                    navController = navController,
                    loggedInUser = mainUiState.loggedInUser
                )
            }
        }, lifecycleOwner
    ) {

        lifecycleOwner.addObserver(connectionObserver)

        onDispose {
            lifecycleOwner.removeObserver(connectionObserver)
        }
    }
}