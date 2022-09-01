package com.hanialjti.allchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.rememberBottomSheetNavigator
import com.hanialjti.allchat.ui.NavigationLayout
import com.hanialjti.allchat.ui.skin.AllChatChatScreenSkin
import com.hanialjti.allchat.ui.skin.AllChatConversationScreenSkin
import com.hanialjti.allchat.ui.skin.DefaultChatScreenSkin
import com.hanialjti.allchat.ui.skin.DefaultConversationScreenSkin
import com.hanialjti.allchat.ui.theme.AllChatTheme
import com.hanialjti.allchat.xmpp.XmppConnectionHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            val lifecycle by rememberUpdatedState(newValue = LocalLifecycleOwner.current)

            AllChat(
                chatSkin = DefaultChatScreenSkin,
                conversationSkin = DefaultConversationScreenSkin,
                lifecycleOwner = lifecycle,
                connectionManager = XmppConnectionHelper
            )
//            val bottomSheetNavigator = rememberBottomSheetNavigator()
//            val navController = rememberNavController(bottomSheetNavigator)

//            AllChatTheme {
//                Box {
//                    Image(
//                        painter = painterResource(id = R.drawable.background),
//                        contentDescription = null,
//                        modifier = Modifier.fillMaxSize(),
//                        contentScale = ContentScale.Crop
//                    )
//                    NavigationLayout(
//                        bottomSheetNavigator = bottomSheetNavigator,
//                        navController = navController,
//                    )
//                }
//            }
        }
    }
}


@OptIn(ExperimentalMaterialNavigationApi::class)
@Composable
fun AllChat(
    chatSkin: AllChatChatScreenSkin,
    conversationSkin: AllChatConversationScreenSkin,
    lifecycleOwner: LifecycleOwner,
    connectionManager: ConnectionManager
) {

    val bottomSheetNavigator = rememberBottomSheetNavigator()
    val navController = rememberNavController(bottomSheetNavigator)

    DisposableEffect(
        AllChatTheme {
            Box {
                Image(
                    painter = painterResource(id = R.drawable.background),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                NavigationLayout(
                    bottomSheetNavigator = bottomSheetNavigator,
                    navController = navController,
                )
            }
        },
        lifecycleOwner
    ) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    lifecycleOwner.lifecycleScope.launch {
                        connectionManager.connect("hani", "15960400")
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    lifecycleOwner.lifecycleScope.launch {
                        connectionManager.disconnect()
                    }
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
