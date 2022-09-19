package com.hanialjti.allchat

import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.rememberBottomSheetNavigator
import com.hanialjti.allchat.datastore.UserPreferencesManager
import com.hanialjti.allchat.di.appModule
import com.hanialjti.allchat.exception.NotInitializedException
import com.hanialjti.allchat.models.UserCredentials
import com.hanialjti.allchat.ui.NavigationLayout
import com.hanialjti.allchat.ui.skin.AllChatChatScreenSkin
import com.hanialjti.allchat.ui.skin.AllChatConversationScreenSkin
import com.hanialjti.allchat.ui.skin.DefaultChatScreenSkin
import com.hanialjti.allchat.ui.skin.DefaultConversationScreenSkin
import com.hanialjti.allchat.ui.theme.AllChatTheme
import com.hanialjti.allchat.viewmodels.MainViewModel
import com.hanialjti.allchat.xmpp.XmppConnectionCredentials
import com.hanialjti.allchat.xmpp.xmppModule
import kotlinx.coroutines.flow.collectLatest
import org.jivesoftware.smack.packet.Message
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.compose.get
import org.koin.androidx.compose.getViewModel
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.logger.Level
import org.koin.dsl.koinApplication

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val lifecycle = lifecycle

        initializeAllChat(
            this,
            ConnectionType.Xmpp(
                XmppConnectionCredentials(
                    host = "192.168.0.42",
                    domain = "localhost",
                    port = 5222
                )
            )
        )

        setContent {

            AllChat(
                chatSkin = DefaultChatScreenSkin,
                conversationSkin = DefaultConversationScreenSkin,
                lifecycleOwner = lifecycle
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
    val mainViewModel = CustomKoin.get<MainViewModel>()
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

object MyKoinContext {
    var koinApp: KoinApplication? = null
}

fun initializeAllChat(
    context: Context,
    connectionType: ConnectionType
) {
    MyKoinContext.koinApp = koinApplication {
        androidContext(context)
        workManagerFactory()
        modules(appModule)
        printLogger(Level.INFO)
        when (connectionType) {
            is ConnectionType.Xmpp -> {
                koin.declare(connectionType.connectionCredentials)
                koin.loadModules(listOf(xmppModule))
            }
        }
    }
}

abstract class CustomKoinComponent : KoinComponent {
    override fun getKoin(): Koin = MyKoinContext.koinApp?.koin
        ?: throw NotInitializedException("Please call initializeAllChat() first!")
}

object CustomKoin : CustomKoinComponent() {

    @OptIn(KoinInternalApi::class)
    fun getScope() = getKoin().scopeRegistry.rootScope
}

