package com.hanialjti.allchat.presentation

import android.app.Activity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.hanialjti.allchat.data.local.datastore.UserPreferencesManager
import com.hanialjti.allchat.data.remote.ConnectionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ConnectionLifeCycleObserver(
    private val connectionManager: ConnectionManager,
    private val userPreferencesManager: UserPreferencesManager,
    private val applicationScope: CoroutineScope
): DefaultLifecycleObserver {
    private var isInBackground = true

    override fun onResume(owner: LifecycleOwner) {
        if (owner is Activity && !owner.isChangingConfigurations && isInBackground) {
            isInBackground = false
            applicationScope.launch {
                userPreferencesManager.userCredentials
                    .collectLatest { userCredentials ->
                        if (userCredentials != null) {
                            connectionManager.connect(userCredentials)
                        }
                    }
            }
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        when (owner) {
            is Activity -> {
                if (!owner.isChangingConfigurations) {
                    isInBackground = true
                    applicationScope.launch { connectionManager.disconnect() }
                }
            }
        }
    }

}