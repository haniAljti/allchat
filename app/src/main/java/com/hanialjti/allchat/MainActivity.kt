package com.hanialjti.allchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.rememberBottomSheetNavigator
import com.hanialjti.allchat.models.UserCredentials
import com.hanialjti.allchat.ui.NavigationLayout
import com.hanialjti.allchat.ui.skin.AllChatChatScreenSkin
import com.hanialjti.allchat.ui.skin.AllChatConversationScreenSkin
import com.hanialjti.allchat.ui.skin.DefaultChatScreenSkin
import com.hanialjti.allchat.ui.skin.DefaultConversationScreenSkin
import com.hanialjti.allchat.ui.theme.AllChatTheme
import com.hanialjti.allchat.viewmodels.MainViewModel
import com.hanialjti.allchat.getViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val lifecycle = lifecycle

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
    val mainUiState by remember(mainViewModel) {
        mainViewModel.uiState
    }.collectAsState()

    LaunchedEffect(userCredentials) {
        if (userCredentials != null) {
            mainViewModel.updateUserCredentials(userCredentials)
        }
    }

    DisposableEffect(
        AllChatTheme {
            NavigationLayout(
                modifier = Modifier.background(MaterialTheme.colors.background),
                bottomSheetNavigator = bottomSheetNavigator,
                navController = navController,
                userCredentials = mainUiState.userCredentials
            )
        }
    , lifecycleOwner
    ) {

        lifecycleOwner.addObserver(mainViewModel)

        onDispose {
            lifecycleOwner.removeObserver(mainViewModel)
        }
    }
}