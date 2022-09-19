package com.hanialjti.allchat.xmpp

import com.hanialjti.allchat.ConnectionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jivesoftware.smack.ConnectionListener
import org.jivesoftware.smack.XMPPConnection
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import timber.log.Timber

class XmppConnectionManager(
    private val connection: XMPPTCPConnection
) : ConnectionManager, ConnectionListener {

    override fun observeConnectivityStatus(): Flow<ConnectionManager.Status> = callbackFlow {

        val initialStatus = if (connection.isAuthenticated) ConnectionManager.Status.Connected else
            ConnectionManager.Status.Disconnected

        send(initialStatus)

        val connectionListener = object : ConnectionListener {
            override fun authenticated(connection: XMPPConnection?, resumed: Boolean) {
                launch { send(ConnectionManager.Status.Connected) }
            }

            override fun connectionClosed() {
                Timber.d("Xmpp disconnected")
                launch { send(ConnectionManager.Status.Disconnected) }
            }
        }

        connection.addConnectionListener(connectionListener)

        awaitClose {
            connection.removeConnectionListener(connectionListener)
        }

    }.distinctUntilChanged()

    override suspend fun connect(username: String, password: String) = withContext(Dispatchers.IO) {
        try {
            connection.connect().login(username, password)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            connection.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}