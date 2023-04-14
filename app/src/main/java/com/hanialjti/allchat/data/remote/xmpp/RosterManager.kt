package com.hanialjti.allchat.data.remote.xmpp

import com.hanialjti.allchat.common.utils.Logger
import com.hanialjti.allchat.data.remote.xmpp.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jivesoftware.smack.SmackException
import org.jivesoftware.smack.StanzaListener
import org.jivesoftware.smack.XMPPException
import org.jivesoftware.smack.filter.*
import org.jivesoftware.smack.packet.Presence
import org.jivesoftware.smack.roster.Roster
import org.jivesoftware.smack.roster.RosterListener
import org.jivesoftware.smack.roster.SubscribeListener
import org.jivesoftware.smack.roster.SubscribeListener.SubscribeAnswer
import org.jivesoftware.smack.roster.rosterstore.RosterStore
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smackx.iqlast.LastActivityManager
import org.jivesoftware.smackx.iqprivate.PrivateDataManager
import org.jivesoftware.smackx.muc.packet.MUCInitialPresence
import org.jivesoftware.smackx.muc.packet.MUCUser
import org.jxmpp.jid.Jid
import org.jxmpp.jid.impl.JidCreate
import timber.log.Timber
import java.time.Instant
import java.time.ZoneOffset

class RosterManager(
    connection: XMPPTCPConnection,
    rosterStore: RosterStore,
    externalScope: CoroutineScope,
    private val roster: Roster = Roster.getInstanceFor(connection),
    private val lastActivityManager: LastActivityManager = LastActivityManager
        .getInstanceFor(connection)
) {

    suspend fun getUserPresence(userId: String): Presence? = withContext(Dispatchers.IO) {
        return@withContext try {
            roster.getPresence(JidCreate.entityBareFrom(userId))
        } catch (e: SmackException.NoResponseException) {
            Timber.e(e)
            null
        } catch (e: XMPPException) {
            Timber.e(e)
            null
        } catch (e: SmackException.NotConnectedException) {
            Timber.e(e)
            null
        } catch (e: InterruptedException) {
            Timber.e(e)
            null
        }
    }


    private val inboundPresenceSubscriptionStream = callbackFlow {

        val inboundPresencesSubscriptionListener = SubscribeListener { from, _ ->
            launch {
                send(PresenceSubscriptionArrived(from.asBareJid().toString()))
            }
            SubscribeAnswer.Approve
        }

        roster.addSubscribeListener(inboundPresencesSubscriptionListener)

        awaitClose { roster.removeSubscribeListener(inboundPresencesSubscriptionListener) }
    }

    private val listenForPresenceUpdates = callbackFlow {

        fun updateAndSendPresence(presence: Presence) {
            val highestPriorityPresence = roster.getPresence(presence.from?.asBareJid())
            val userId = presence.from?.asBareJid()?.toString()
            val status = highestPriorityPresence?.status
            val isOnline = highestPriorityPresence.isAvailable && !highestPriorityPresence.isAway

            if (userId != null) {
                launch(Dispatchers.IO) {
                    kotlinx.coroutines.delay(500)
                    val lastOnline = if (isOnline) null else lastActivity(userId)
                    send(
                        PresenceUpdated(
                            userId = userId,
                            isOnline = isOnline,
                            status = status,
                            lastOnline = lastOnline?.let {
                                Instant.ofEpochMilli(it).atOffset(
                                    ZoneOffset.UTC
                                )
                            }
                        )
                    )
                }
            }
        }

        val presenceListener = StanzaListener { stanza ->
            Logger.d { "New Presence from ${stanza.from.asBareJid()}" }
            val presence = stanza as Presence

            when (presence.type) {
                Presence.Type.available, Presence.Type.unavailable -> {
                    updateAndSendPresence(presence)
                }
                Presence.Type.subscribed -> {
                    // Presence request approved
                    PresenceSubscriptionApproved(presence.from.toString())
                }
                Presence.Type.unsubscribed -> {
                    // Presence request denied Or subscriber unsubscribed
                    PresenceSubscriptionDenied(presence.from.toString())
                }
                else -> {}
            }
        }

//        val presenceEventListener = object : PresenceEventListener {
//            override fun presenceAvailable(address: FullJid?, availablePresence: Presence) {
//                updateAndSendPresence(availablePresence)
//            }
//
//            override fun presenceUnavailable(address: FullJid?, presence: Presence) {
//                updateAndSendPresence(presence)
//            }
//
//            override fun presenceError(address: Jid?, errorPresence: Presence) {}
//
//            override fun presenceSubscribed(address: BareJid?, subscribedPresence: Presence) {
//                // Presence request approved
//                PresenceSubscriptionApproved(address.toString())
//            }
//
//            override fun presenceUnsubscribed(address: BareJid?, unsubscribedPresence: Presence) {
//                // Presence request denied
//                PresenceSubscriptionDenied(address.toString())
//            }
//
//        }

        connection.addStanzaListener(
            presenceListener,
            AndFilter(
                StanzaTypeFilter.PRESENCE,
                NotFilter(
                    MessageTypeFilter.ERROR
                ),
                NotFilter(
                    OrFilter(
                        StanzaExtensionFilter(MUCUser.NAMESPACE),
                        StanzaExtensionFilter(MUCInitialPresence.NAMESPACE)
                    )
                )
            )
        )

        awaitClose { connection.removeStanzaListener(presenceListener) }
    }

    private val rosterStream = callbackFlow {

        var isInit = false

        roster.entries.forEach {
            send(ItemAdded(it.jid.asBareJid().toString()))
        }

        val rosterListener = object : RosterListener {

            override fun entriesAdded(addresses: MutableCollection<Jid>) {

                if (isInit) { // this will be loaded with the synchronizer
                    launch {
                        addresses.forEach { jid ->
                            Logger.d { "New roster entry is received with jid: $jid" }
                            send(ItemAdded(jid.asBareJid().toString()))
                        }
                    }
                }
                isInit = true
            }

            override fun entriesUpdated(addresses: MutableCollection<Jid>) {

                launch {
                    addresses.forEach { jid ->
                        Logger.d {
                            "Updated roster entry is received with jid: $jid"
                        }
                        send(
                            ItemUpdated(jid.asBareJid().toString())
                        )
                    }
                }

            }

            override fun entriesDeleted(addresses: MutableCollection<Jid>) {
                Logger.d {
                    "Deleted roster entries are received with jids: ${
                        addresses.map {
                            it.asBareJid().toString()
                        }
                    }"
                }

                launch {
                    addresses.forEach { jid ->
                        send(
                            ItemDeleted(jid.asBareJid().toString())
                        )
                    }
                }
            }

            override fun presenceChanged(presence: Presence?) {}
        }

        roster.addRosterListener(rosterListener)
        roster.setRosterStore(rosterStore)
        awaitClose { roster.removeRosterListener(rosterListener) }
    }

    val rosterUpdateStream: SharedFlow<RosterUpdate> = merge(
        inboundPresenceSubscriptionStream,
        listenForPresenceUpdates,
        rosterStream
    ).shareIn(externalScope, SharingStarted.WhileSubscribed(), replay = 1000)

    private suspend fun lastActivity(userId: String) = withContext(Dispatchers.IO) {
        return@withContext try {
            val lastActivity = lastActivityManager.getLastActivity(userId.asJid())
            lastActivity.lastActivity
        } catch (e: XMPPException) {
            Timber.e(e)
            null
        } catch (e: SmackException.NoResponseException) {
            Timber.e(e)
            null
        } catch (e: SmackException.NotConnectedException) {
            Timber.e(e)
            null
        } catch (e: InterruptedException) {
            Timber.e(e)
            null
        }
    }

    suspend fun createRosterItemAndSendSubscriptionRequest(userId: String) {
        createRosterItem(userId)
        sendPresenceSubscriptionRequest(userId)
    }

    private suspend fun createRosterItem(userId: String) {
        try {
            roster.createItem(userId.asJid(), null, null)
        } catch (e: Exception) {
            Logger.e(e)
        }
    }

    private suspend fun sendPresenceSubscriptionRequest(userId: String) {
        try {
            val jid = userId.asJid()
            if (!roster.iAmSubscribedTo(jid)) {
                roster.sendSubscriptionRequest(jid)
            }
        } catch (e: Exception) {
            Logger.e(e)
        }
    }
}