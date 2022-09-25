package com.hanialjti.allchat.di

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.work.Configuration
import androidx.work.DelegatingWorkerFactory
import androidx.work.WorkManager
import com.hanialjti.allchat.data.remote.ConnectionType
import org.koin.android.ext.koin.androidContext
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.component.KoinComponent
import org.koin.core.context.GlobalContext
import org.koin.core.logger.Level
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import org.koin.core.scope.Scope
import org.koin.dsl.koinApplication

object AllChat {
    private var koinApp: KoinApplication? = null

    fun getKoinApplication(): KoinApplication = koinApp
        ?: error("Please call AllChat.initialize() in Application#onCreate() first!")

    fun initialize(
        context: Context,
        connectionType: ConnectionType
    ) {
        koinApp = koinApplication {
            androidContext(context)
            customWorkManagerFactory()
            modules(appModule, workerFactoryModule)
            printLogger(Level.INFO)
            when (connectionType) {
                is ConnectionType.Xmpp -> {
                    koin.declare(connectionType.connectionCredentials)
                    koin.loadModules(listOf(xmppModule))
                }
            }
        }

    }

    fun stop() {
        koinApp?.close()
        koinApp = null
    }

}

fun KoinApplication.customWorkManagerFactory() {
    createCustomWorkManagerFactory()
}

private fun KoinApplication.createCustomWorkManagerFactory() {
    val factory = DelegatingWorkerFactory()
        .apply {
            addFactory(LocalContextWorkerFactory())
        }

    val conf = Configuration.Builder()
        .setWorkerFactory(factory)
        .build()

    WorkManager.initialize(koin.get(), conf)
}

@OptIn(KoinInternalApi::class)
@Composable
inline fun <reified T : ViewModel> getViewModel(
    qualifier: Qualifier? = null,
    owner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    },
    scope: Scope = AllChat.getKoinApplication().koin.scopeRegistry.rootScope,
    noinline parameters: ParametersDefinition? = null
): T {
    return org.koin.androidx.compose.getViewModel(qualifier, owner, scope, parameters)
}

@OptIn(KoinInternalApi::class)
@Composable
inline fun <reified T> get(
    qualifier: Qualifier? = null,
    scope: Scope = AllChat.getKoinApplication().koin.scopeRegistry.rootScope,
    noinline parameters: ParametersDefinition? = null,
): T = remember(qualifier, parameters) {
    scope.get(qualifier, parameters)
}

interface CustomKoinComponent : KoinComponent {
    override fun getKoin(): Koin = AllChat.getKoinApplication().koin
}