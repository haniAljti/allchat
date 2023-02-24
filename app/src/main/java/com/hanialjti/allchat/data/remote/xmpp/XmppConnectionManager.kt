package com.hanialjti.allchat.data.remote.xmpp

import android.content.Context
import androidx.work.ListenableWorker
import com.hanialjti.allchat.common.exception.NotAuthenticatedException
import com.hanialjti.allchat.common.utils.Logger
import com.hanialjti.allchat.data.local.datastore.UserCredentials
import com.hanialjti.allchat.data.local.datastore.PreferencesLocalDataStore
import com.hanialjti.allchat.data.remote.ConnectionManager
import com.hanialjti.allchat.data.remote.delayRetry
import com.hanialjti.allchat.data.remote.model.Presence
import com.hanialjti.allchat.data.remote.xmpp.model.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jivesoftware.smack.ConnectionListener
import org.jivesoftware.smack.ReconnectionManager
import org.jivesoftware.smack.SmackException
import org.jivesoftware.smack.XMPPConnection
import org.jivesoftware.smack.XMPPException.XMPPErrorException
import org.jivesoftware.smack.provider.ProviderManager
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smackx.bookmarks.BookmarkManager
import org.jivesoftware.smackx.caps.EntityCapsManager
import org.jivesoftware.smackx.caps.cache.SimpleDirectoryPersistentCache
import org.jivesoftware.smackx.carbons.CarbonManager
import org.jivesoftware.smackx.chat_markers.element.ChatMarkersElements
import org.jivesoftware.smackx.csi.ClientStateIndicationManager
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager
import org.jivesoftware.smackx.muc.MultiUserChatManager
import org.jivesoftware.smackx.nick.packet.Nick
import org.jxmpp.jid.parts.Resourcepart
import timber.log.Timber

class XmppConnectionManager(
    appContext: Context,
    private val connection: XMPPTCPConnection,
    private val preferencesLocalDataStore: PreferencesLocalDataStore,
    private val externalScope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    private val config: XmppConnectionConfig
) : ConnectionManager, ConnectionListener {

    private val workers: MutableSet<ListenableWorker> = mutableSetOf()
    private var connectRequested: Boolean = false

    private val reconnectionManager = ReconnectionManager.getInstanceFor(connection)
    private val carbonManager = CarbonManager.getInstanceFor(connection)
    private val bookmarkManager = BookmarkManager.getBookmarkManager(connection)
    private val mucManager = MultiUserChatManager.getInstanceFor(connection)

    //    private val entityCapsManager = EntityCapsManager.getInstanceFor(connection)
    private val entityCapsPersistentCache = SimpleDirectoryPersistentCache(appContext.cacheDir)
    private val serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(connection)
    private val serverPingWithAlarmManager = ServerPingWithAlarmManager.getInstanceFor(connection)

    init {
        ProviderManager.addExtensionProvider(
            OutOfBandData.ELEMENT,
            OutOfBandData.NAMESPACE,
            OOBExtensionProvider()
        )
        ProviderManager.addExtensionProvider(
            AvatarDataExtensionElement.ELEMENT_NAME,
            AvatarDataExtensionElement.NAMESPACE,
            AvatarExtensionProvider()
        )
        ProviderManager.addExtensionProvider(
            AvatarMetaDataExtensionElement.ELEMENT_NAME,
            AvatarMetaDataExtensionElement.NAMESPACE,
            AvatarMetaDataExtensionProvider()
        )
        if (config.chatMarkersEnabled) {
            serviceDiscoveryManager.addFeature(ChatMarkersElements.NAMESPACE)
        }
        serviceDiscoveryManager.addFeature(Nick.NAMESPACE)
        serviceDiscoveryManager.addFeature(AvatarMetaDataExtensionElement.NAMESPACE)
        serviceDiscoveryManager.addFeature(Nick.NAMESPACE.plus("+notify"))
        serviceDiscoveryManager.addFeature(AvatarMetaDataExtensionElement.NAMESPACE.plus("+notify"))
        EntityCapsManager.setPersistentCache(entityCapsPersistentCache)
        serverPingWithAlarmManager.isEnabled = true
    }

    override val userId get() = connection.user?.asBareJid()?.toString()

    override val clientId get() = connection.user?.resourceOrNull?.toString()

    override fun getConfig() = config

    override val loggedInUser = observeConnectivityStatus()
        .map { connectionStatus ->
            if (connectionStatus == ConnectionManager.Status.Connected) {
                connection.user?.asBareJid()?.toString()
            } else null
        }

    private suspend fun joinAllChatRooms() {
        try {
            bookmarkManager.bookmarkedConferences.forEach {
                if (it.isAutoJoin) {
                    joinRoom(it.jid.asBareJid().toString(), connection.user.asBareJid().toString())
                }
            }
        } catch (e: XMPPErrorException) {
            Timber.e(e)
        } catch (e: SmackException.NoResponseException) {
            Timber.e(e)
        } catch (e: SmackException.NotConnectedException) {
            Timber.e(e)
        } catch (e: InterruptedException) {
            Timber.e(e)
        }
    }

    private suspend fun joinRoom(roomId: String, myId: String) {
        val muc = mucManager.getMultiUserChat(roomId.asJid().asEntityBareJidIfPossible())
        if (!muc.isJoined) {
            try {
                val history = muc.getEnterConfigurationBuilder(Resourcepart.from(myId))
                    .requestHistorySince(534776876).build()
                muc.join(history)
            } catch (e: XMPPErrorException) {
                Timber.e(e)
            } catch (e: SmackException.NoResponseException) {
                Timber.e(e)
            } catch (e: SmackException.NotConnectedException) {
                Timber.e(e)
            } catch (e: InterruptedException) {
                Timber.e(e)
            }
        }
    }

    override suspend fun registerWorker(worker: ListenableWorker): Unit = withContext(dispatcher) {
        workers.add(worker)
        preferencesLocalDataStore.userCredentials.first().let {
            if (it != null) {
                login(it)
            } else {
                throw NotAuthenticatedException("User credentials are null", null)
            }
        }
    }

    override suspend fun updateMyPresence(presence: Presence): Unit = withContext(dispatcher) {
        val presenceStanza = connection.stanzaFactory.buildPresenceStanza()
//            .ofType(
//                if (presence.type == Presence.Type.Available) org.jivesoftware.smack.packet.Presence.Type.available
//                else org.jivesoftware.smack.packet.Presence.Type.unavailable
//            )
            .setStatus(presence.status)
            .setMode(
                if (presence.type == Presence.Type.Available) org.jivesoftware.smack.packet.Presence.Mode.available
                else org.jivesoftware.smack.packet.Presence.Mode.away
            )
            .build()

        try {
            connection.sendStanza(presenceStanza)
        } catch (e: SmackException.NotConnectedException) {
            Timber.e(e)
        } catch (e: InterruptedException) {
            Timber.e(e)
        }

    }

    override suspend fun onResume() {
        Logger.d { "signing in..." }
        connectRequested = true
        preferencesLocalDataStore.userCredentials.first()?.let { connect(it) }
        connection.addConnectionListener(this)
        updateMyPresence(Presence(Presence.Type.Available, null))
        if (config.useForegroundService && ClientStateIndicationManager.isSupported(connection)) {
            try {
                ClientStateIndicationManager.active(connection)
            } catch (e: Exception) {
                Logger.e(e)
            }
        }
    }

    override suspend fun onPause() {
        Logger.d { "logging out..." }
        connectRequested = false
        if (config.useForegroundService && ClientStateIndicationManager.isSupported(connection)) {
            try {
                ClientStateIndicationManager.inactive(connection)
            } catch (e: Exception) {
                Logger.e(e)
            }
        }
//        entityCapsManager.disableEntityCaps()
        reconnectionManager.disableAutomaticReconnection()
        connection.removeConnectionListener(this)
        updateMyPresence(Presence(Presence.Type.Unavailable, null))

    }

    override suspend fun unregisterWorker(worker: ListenableWorker): Unit =
        withContext(dispatcher) {
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
            } else throw NotAuthenticatedException("Credentials are null", null)
        }
    }

    private suspend fun disconnectIfPossible() = withContext(dispatcher) {
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
        disconnectIfPossible()
    }

    override fun authenticated(connection: XMPPConnection?, resumed: Boolean) {
        externalScope.launch { joinAllChatRooms() }
    }

}