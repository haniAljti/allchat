package com.hanialjti.allchat.presentation

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle

@Composable
inline fun <reified BoundService : Service, reified BoundServiceBinder : Binder> rememberBoundLocalService(
    lifecycle: Lifecycle,
    crossinline getService: @DisallowComposableCalls BoundServiceBinder.() -> BoundService,
): BoundService? {
    val context: Context = LocalContext.current
    var boundService: BoundService? by remember(context) { mutableStateOf(null) }
    val serviceConnection: ServiceConnection = remember(context) {
        object : ServiceConnection {
            override fun onServiceConnected(className: ComponentName, service: IBinder) {
                boundService = (service as BoundServiceBinder).getService()
            }

            override fun onServiceDisconnected(arg0: ComponentName) {
                boundService = null
            }
        }
    }
    DisposableEffect(context, serviceConnection, lifecycle) {
        context.bindService(Intent(context, BoundService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
        onDispose { context.unbindService(serviceConnection) }
    }
    return boundService
}