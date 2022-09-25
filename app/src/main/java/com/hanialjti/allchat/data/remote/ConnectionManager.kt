package com.hanialjti.allchat.data.remote

import android.app.Activity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.work.ListenableWorker
import com.hanialjti.allchat.data.local.datastore.UserCredentials
import com.hanialjti.allchat.data.local.datastore.UserPreferencesManager
import com.hanialjti.allchat.data.remote.xmpp.XmppConnectionConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface ConnectionManager: DefaultLifecycleObserver {

    fun observeConnectivityStatus(): Flow<Status>
    suspend fun connect(userCredentials: UserCredentials)
    suspend fun disconnect()
    suspend fun registerWorker(worker: ListenableWorker)
    suspend fun unregisterWorker(worker: ListenableWorker)

    enum class Status { Connected, Disconnected }
}

sealed class ConnectionType {
    class Xmpp(val connectionCredentials: XmppConnectionConfig): ConnectionType()
    class Firebase(): ConnectionType()
}