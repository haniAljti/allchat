package com.hanialjti.allchat

import com.hanialjti.allchat.xmpp.XmppConnectionCredentials
import kotlinx.coroutines.flow.Flow

interface ConnectionManager {
    fun observeConnectivityStatus(): Flow<Status>
    suspend fun connect(username: String, password: String)
    suspend fun disconnect()

    enum class Status { Connected, Disconnected }
}

sealed class ConnectionType {
    class Xmpp(val connectionCredentials: XmppConnectionCredentials): ConnectionType()
    class Firebase(): ConnectionType()
}