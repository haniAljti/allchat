package com.hanialjti.allchat.data.remote.xmpp

import androidx.work.ListenableWorker
import com.hanialjti.allchat.common.exception.NotAuthenticatedException
import com.hanialjti.allchat.data.local.datastore.UserCredentials
import com.hanialjti.allchat.data.local.datastore.UserPreferencesManager
import com.hanialjti.allchat.data.remote.ConnectionManager
import com.hanialjti.allchat.data.remote.delayRetry
import com.hanialjti.allchat.data.remote.model.Presence
import com.hanialjti.allchat.data.remote.xmpp.model.XmppConnectionConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jivesoftware.smack.ConnectionListener
import org.jivesoftware.smack.ReconnectionManager
import org.jivesoftware.smack.XMPPConnection
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smackx.bookmarks.BookmarkManager
import org.jivesoftware.smackx.carbons.CarbonManager
import org.jivesoftware.smackx.muc.MultiUserChatManager
import org.jivesoftware.smackx.ping.PingManager
import org.jxmpp.jid.parts.Resourcepart
import timber.log.Timber

class XmppConnectionManager(
    private val connection: XMPPTCPConnection,
    private val userPreferencesManager: UserPreferencesManager,
    private val dispatcher: CoroutineDispatcher,
    private val config: XmppConnectionConfig
) : ConnectionManager {

    private val workers: MutableSet<ListenableWorker> = mutableSetOf()
    private var connectRequested: Boolean = false

    private val reconnectionManager = ReconnectionManager.getInstanceFor(connection)
    private val pingManager = PingManager.getInstanceFor(connection)
    private val carbonManager = CarbonManager.getInstanceFor(connection)
    private val bookmarkManager = BookmarkManager.getBookmarkManager(connection)
    private val mucManager = MultiUserChatManager.getInstanceFor(connection)

    override fun getUsername() = connection.user?.asBareJid()?.toString()

    override fun getConfig() = config

    private suspend fun joinAllChatRooms() {
        bookmarkManager.bookmarkedConferences.forEach {
            if (it.isAutoJoin)
            joinRoom(it.jid.asBareJid().toString(), connection.user.asBareJid().toString())
        }
    }

    private suspend fun joinRoom(roomId: String, myId: String) {
        val muc = mucManager.getMultiUserChat(roomId.asJid().asEntityBareJidIfPossible())
        if (!muc.isJoined) {
            val history = muc.getEnterConfigurationBuilder(Resourcepart.from(myId))
                .requestHistorySince(534776876).build()
            muc.join(history)
        }
    }

    override suspend fun registerWorker(worker: ListenableWorker): Unit = withContext(dispatcher) {
        workers.add(worker)
        userPreferencesManager.userCredentials.first().let {
            if (it != null) {
                login(it)
            } else {
                throw NotAuthenticatedException("User credentials are null", null)
            }
        }
    }

    override suspend fun updateMyPresence(presence: Presence): Unit = withContext(dispatcher) {
        connection.stanzaFactory.buildPresenceStanza()
            .ofType(
                if (presence.type == Presence.Type.Available) org.jivesoftware.smack.packet.Presence.Type.available
                else org.jivesoftware.smack.packet.Presence.Type.unavailable
            )
            .setStatus(presence.status)
            .build()
            .also {
                try {
                    connection.sendStanza(it)
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
    }

    override suspend fun onResume() {
        Timber.d("signing in...")
        connectRequested = true
        userPreferencesManager.userCredentials.first()?.let { connect(it) }
        joinAllChatRooms()
        updateMyPresence(Presence(Presence.Type.Available, null))
        pingManager.pingInterval = 60 * 5
//        ClientStateIndicationManager.active(connection)
    }

    override suspend fun onPause() {
        connectRequested = false
        updateMyPresence(Presence(Presence.Type.Unavailable, null))
        pingManager.pingInterval = -1
        reconnectionManager.disableAutomaticReconnection()
//        ClientStateIndicationManager.inactive(connection)
    }

    override suspend fun unregisterWorker(worker: ListenableWorker): Unit = withContext(dispatcher) {
        workers.remove(worker)
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

    private suspend fun login(userCredentials: UserCredentials) = withContext(dispatcher) {
        if (!connection.isAuthenticated) {
            if (userCredentials.username != null && userCredentials.password != null) {
                connection
                    .connect()
                    .login(
                        userCredentials.username,
                        userCredentials.password
                    )
                carbonManager.enableCarbons()
            }
            else throw NotAuthenticatedException("Credentials are null", null)
        }
    }

    private suspend fun disconnectIfNecessary() = withContext(dispatcher) {
        if (connection.isConnected && workers.isEmpty() && !connectRequested) {
            try {
                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun connect(userCredentials: UserCredentials) {
        delayRetry(
            func = { login(userCredentials) },
            breakWhen = { !connectRequested },
            onError = { Timber.e(it) },
            maxRetryCount = Int.MAX_VALUE,
        )
        reconnectionManager.setReconnectionPolicy(ReconnectionManager.ReconnectionPolicy.FIXED_DELAY)
        reconnectionManager.enableAutomaticReconnection()
    }

    override suspend fun disconnect() {
        disconnectIfNecessary()
    }

}