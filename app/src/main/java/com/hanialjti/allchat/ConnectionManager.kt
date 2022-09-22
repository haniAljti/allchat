package com.hanialjti.allchat

import androidx.work.ListenableWorker
import com.hanialjti.allchat.models.UserCredentials
import com.hanialjti.allchat.xmpp.XmppConnectionConfig
import kotlinx.coroutines.flow.Flow

interface ConnectionManager {
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