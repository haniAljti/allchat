package com.hanialjti.allchat.data.remote.xmpp

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.work.ListenableWorker
import com.hanialjti.allchat.data.local.datastore.UserPreferencesManager
import com.hanialjti.allchat.data.remote.ConnectionManager
import com.hanialjti.allchat.data.local.datastore.UserCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jivesoftware.smack.ConnectionListener
import org.jivesoftware.smack.XMPPConnection
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smackx.caps.EntityCapsManager
import org.jivesoftware.smackx.carbons.CarbonManager
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager
import timber.log.Timber

class XmppConnectionManager(
    private val connection: XMPPTCPConnection,
    private val userPreferencesManager: UserPreferencesManager,
    private val carbonManager: CarbonManager
) : ConnectionManager, DefaultLifecycleObserver {

    private val workers: MutableList<ListenableWorker> = mutableListOf()
    private var connectRequested: Boolean = false

    override suspend fun registerWorker(worker: ListenableWorker): Unit =
        withContext(Dispatchers.IO) {
        workers.add(worker)
        userPreferencesManager.userCredentials.first()?.let {
            login(it)
        }
    }

    override suspend fun unregisterWorker(worker: ListenableWorker) = withContext(Dispatchers.IO) {
        workers.remove(worker)
        disconnectConnection()
    }

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

    private suspend fun login(userCredentials: UserCredentials) = withContext(Dispatchers.IO) {
        if (!connection.isAuthenticated) {
            if (userCredentials.username != null && userCredentials.password != null) {
                try {
                    connection.connect().login(userCredentials.username, userCredentials.password)
                    carbonManager.enableCarbons()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private suspend fun disconnectConnection() = withContext(Dispatchers.IO) {
        if (connection.isAuthenticated && workers.isEmpty() && !connectRequested) {
            try {
                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun connect(userCredentials: UserCredentials) = withContext(Dispatchers.IO) {
        connectRequested = true
        login(userCredentials)
    }


    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        connectRequested = false
        disconnectConnection()
    }

}