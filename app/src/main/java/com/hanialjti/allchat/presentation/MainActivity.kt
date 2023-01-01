package com.hanialjti.allchat.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.rememberBottomSheetNavigator
import com.hanialjti.allchat.di.getViewModel
import com.hanialjti.allchat.data.local.datastore.UserCredentials
import com.hanialjti.allchat.di.get
import com.hanialjti.allchat.presentation.ui.NavigationLayout
import com.hanialjti.allchat.presentation.ui.skin.AllChatChatScreenSkin
import com.hanialjti.allchat.presentation.ui.skin.AllChatConversationScreenSkin
import com.hanialjti.allchat.presentation.ui.skin.DefaultChatScreenSkin
import com.hanialjti.allchat.presentation.ui.skin.DefaultConversationScreenSkin
import com.hanialjti.allchat.presentation.ui.theme.AllChatTheme
import org.jivesoftware.smackx.ping.android.ServerPingWithAlarmManager

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
                loggedInUser = mainUiState.loggedInUser
            )
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