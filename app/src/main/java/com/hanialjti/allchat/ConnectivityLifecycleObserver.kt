package com.hanialjti.allchat

import android.app.Activity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.hanialjti.allchat.datastore.UserPreferencesManager
import com.hanialjti.allchat.models.UserCredentials
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ConnectivityLifecycleObserver(
    private val connectionManager: ConnectionManager,
    private val coroutineScope: CoroutineScope,
    private val userPreferencesManager: UserPreferencesManager
) : DefaultLifecycleObserver {

    override fun onResume(owner: LifecycleOwner) {
        if (owner is Activity && !owner.isChangingConfigurations) {
            coroutineScope.launch {
                userPreferencesManager.userCredentials
                    .collectLatest { userCredentials ->
                        if (userCredentials != null) {
                            connect(userCredentials)
                        }
                    }
            }
        }
    }

    private suspend fun connect(userCredentials: UserCredentials?) {
        if (userCredentials?.username != null && userCredentials.password != null) {
            connectionManager.connect(userCredentials)
        }
    }

    private suspend fun disconnect() {
        connectionManager.disconnect()
    }

//    fun updateUserCredentials(userCredentials: UserCredentials?) {
//        this.userCredentials = userCredentials
//        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
//            coroutineScope.launch {
//                connect(userCredentials)
//            }
//        }
//    }

    override fun onStop(owner: LifecycleOwner) {
        when (owner) {
            is Activity -> {
                if (!owner.isChangingConfigurations) {
                    coroutineScope.launch { disconnect() }
                }
            }
        }

    }
}