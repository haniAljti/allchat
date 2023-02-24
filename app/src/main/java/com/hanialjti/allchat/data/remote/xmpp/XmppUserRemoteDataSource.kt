package com.hanialjti.allchat.data.remote.xmpp

import com.hanialjti.allchat.common.model.ListChange
import com.hanialjti.allchat.common.utils.Logger
import com.hanialjti.allchat.data.model.Avatar
import com.hanialjti.allchat.data.remote.UserRemoteDataSource
import com.hanialjti.allchat.data.remote.model.*
import com.hanialjti.allchat.data.remote.xmpp.model.AvatarDataExtensionElement
import com.hanialjti.allchat.data.remote.xmpp.model.AvatarMetaDataExtensionElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jivesoftware.smack.SmackException.NoResponseException
import org.jivesoftware.smack.SmackException.NotConnectedException
import org.jivesoftware.smack.XMPPException
import org.jivesoftware.smack.packet.Presence
import org.jivesoftware.smack.roster.PresenceEventListener
import org.jivesoftware.smack.roster.Roster
import org.jivesoftware.smack.roster.RosterListener
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smackx.blocking.BlockingCommandManager
import org.jivesoftware.smackx.iqlast.LastActivityManager
import org.jivesoftware.smackx.nick.packet.Nick
import org.jivesoftware.smackx.pep.PepEventListener
import org.jivesoftware.smackx.pep.PepManager
import org.jivesoftware.smackx.pubsub.PayloadItem
import org.jivesoftware.smackx.pubsub.PubSubManager
import org.jivesoftware.smackx.vcardtemp.VCardManager
import org.jxmpp.jid.BareJid
import org.jxmpp.jid.FullJid
import org.jxmpp.jid.Jid
import org.jxmpp.jid.impl.JidCreate
import timber.log.Timber
import java.io.UnsupportedEncodingException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.time.Instant
import java.time.ZoneOffset
import java.util.*

class XmppUserRemoteDataSource(
    private val connection: XMPPTCPConnection
) : UserRemoteDataSource {

    private val vCardManager = VCardManager.getInstanceFor(connection)
    private val rosterManager = Roster.getInstanceFor(connection)
    private val lastActivityManager = LastActivityManager.getInstanceFor(connection)
    private val pepManager = PepManager.getInstanceFor(connection)
    private val blockingCommandManager = BlockingCommandManager.getInstanceFor(connection)

    override suspend fun getUpdatedUserInfo(userId: String) = withContext(Dispatchers.IO) {
        return@withContext try {
            val vCard = getUserVCard(userId)
                ?: return@withContext CallResult.Error("Failed to fetch user info")

            val presence = getUserPresence(userId)
                ?: return@withContext CallResult.Error("Failed to fetch user info")

            val lastOnline = if (presence.isAvailable) null else lastActivity(userId)

            val lastOnlineDateTime = lastOnline?.let {
                Instant.ofEpochMilli(it).atOffset(
                    ZoneOffset.UTC
                )
            }

            CallResult.Success(
                FullUserInfo(
                    user = RemoteUser(
                        id = userId,
                        name = vCard.nickName,
                        avatar = vCard.avatar?.let { data ->
                            Avatar.Raw(data)
                        }
                    ),
                    presence = RemotePresence(
                        isOnline = presence.isAvailable,
                        status = presence.status,
                        lastOnline = lastOnlineDateTime
                    )
                )
            )
        } catch (e: NoResponseException) {
            Timber.e(e)
            CallResult.Error("An error occurred while fetching user info", e)
        } catch (e: XMPPException) {
            Timber.e(e)
            CallResult.Error("An error occurred while fetching user info", e)
        } catch (e: NotConnectedException) {
            Timber.e(e)
            CallResult.Error("An error occurred while fetching user info", e)
        } catch (e: InterruptedException) {
            Timber.e(e)
            CallResult.Error("An error occurred while fetching user info", e)
        }
    }

    override suspend fun updateNickname(username: String): CallResult<Boolean> {
        return try {
            pepManager.publish("http://jabber.org/protocol/nick", PayloadItem(Nick(username)))
            CallResult.Success(true)
        } catch (e: Exception) {
            Timber.e(e)
            CallResult.Error("Error")
        }
    }

    override suspend fun updateAvatar(data: ByteArray?): CallResult<Boolean> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                if (pepManager.isSupported) {
                    data?.let {

                        val dataHash = sha1(data)
                            ?: return@withContext CallResult.Error("Enable to hash data")

                        val encodedData = String(Base64.getEncoder().encode(data))

                        pepManager.publish(
                            AvatarDataExtensionElement.NAMESPACE,
                            PayloadItem(
                                dataHash,
                                AvatarDataExtensionElement(data = encodedData)
                            )
                        )

                        pepManager.publish(
                            AvatarMetaDataExtensionElement.NAMESPACE,
                            PayloadItem(
                                dataHash,
                                AvatarMetaDataExtensionElement(
                                    bytes = data.size,
                                    id = dataHash,
                                    height = 96,
                                    width = 96,
                                    type = "image/png"
                                )
                            )
                        )

                    }
                } else {
                    val vCard = vCardManager.loadVCard()
                    vCardManager.saveVCard(
                        vCard.apply {
                            setAvatar(data, "image/png")
                        }
                    )
                }
                CallResult.Success(true)
            } catch (e: Exception) {
                Timber.e(e)
                CallResult.Error("Error")
            }
        }

    private fun sha1(bytes: ByteArray): String? {
        return try {
            val crypt: MessageDigest = MessageDigest.getInstance("SHA-1")
            crypt.reset()
            crypt.update(bytes)
            byteToHex(crypt.digest())
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            null
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
            null
        }
    }

    private fun byteToHex(hash: ByteArray): String {
        val formatter = Formatter()
        for (b in hash) {
            formatter.format("%02x", b)
        }
        val result: String = formatter.toString()
        formatter.close()
        return result
    }

    override fun listenForUserUpdates(): Flow<UserUpdate> = merge(
//        listenForNicknameUpdates(),
//        listenForAvatarUpdates(),
//        listenForPresenceUpdates()
    )


//    private fun listenForNicknameUpdates() = callbackFlow<UserUpdate> {
//        val listener = PepEventListener<Nick> { from, nickname, _, _ ->
//            launch { send(NicknameUpdate(from.asBareJid().toString(), nickname.name)) }
//        }
//
//        pepManager.addPepEventListener(Nick.NAMESPACE, Nick::class.java, listener)
//
//        awaitClose { pepManager.removePepEventListener(listener) }
//    }

//    private fun listenForSubscriptionUpdates() = callbackFlow<UserUpdate> {
//        val listener = PepEventListener<Nick> { from, nickname, _, _ ->
//            launch { send(NicknameUpdate(from.asBareJid().toString(), nickname.name)) }
//        }
//
//        pepManager.addPepEventListener(Nick.NAMESPACE, Nick::class.java, listener)
//
//        awaitClose { pepManager.removePepEventListener(listener) }
//    }

//    private fun listenForAvatarUpdates() = callbackFlow<UserUpdate> {
//        val listener = PepEventListener<AvatarMetaDataExtensionElement> { from, metadata, _, _ ->
//            launch { send(AvatarMetaDataUpdate(from.asBareJid().toString(), metadata.id)) }
//        }
//
//        pepManager.addPepEventListener(
//            AvatarMetaDataExtensionElement.NAMESPACE,
//            AvatarMetaDataExtensionElement::class.java,
//            listener
//        )
//
//        awaitClose { pepManager.removePepEventListener(listener) }
//    }

    private fun listenForPresenceUpdates() = callbackFlow<UserUpdate> {

        val listener = object : RosterListener {
            override fun entriesAdded(addresses: MutableCollection<Jid>?) {

            }

            override fun entriesUpdated(addresses: MutableCollection<Jid>?) {

            }

            override fun entriesDeleted(addresses: MutableCollection<Jid>?) {

            }

            override fun presenceChanged(presence: Presence?) {
                val hightestPriorityPresence = rosterManager.getPresence(presence?.from?.asBareJid())
                val userId = presence?.from?.asBareJid()?.toString()
                val status = hightestPriorityPresence?.status
                val isOnline = !hightestPriorityPresence.isAway

                if (userId != null) {
                    launch {
                        send(
                            PresenceUpdate(
                                userId = userId,
                                presence = RemotePresence(
                                    isOnline = isOnline,
                                    status = status
                                )
                            )
                        )

                    }
                }
            }

        }

        rosterManager.addRosterListener(listener)

        awaitClose { rosterManager.removeRosterListener(listener) }
    }

//    private fun listenForPresenceUpdates() = callbackFlow<UserUpdate> {
//
//        val listener = object : PresenceEventListener {
//            override fun presenceAvailable(address: FullJid?, availablePresence: Presence?) {
//
//                val hightestPriorityPresence = rosterManager.getPresence(address?.asBareJid())
//                val userId = address?.asBareJid()?.toString()
//                val status = hightestPriorityPresence?.status
//                val isOnline = !hightestPriorityPresence.isAway
//
//                if (userId != null) {
//                    launch {
//                        send(
//                            PresenceUpdate(
//                                userId = userId,
//                                presence = RemotePresence(
//                                    isOnline = isOnline,
//                                    status = status
//                                )
//                            )
//                        )
//
//                    }
//                }
//            }
//
//            override fun presenceUnavailable(address: FullJid?, presence: Presence?) {
//                val hightestPriorityPresence = rosterManager.getPresence(address?.asBareJid())
//                val userId = address?.asBareJid()?.toString()
//                val status = hightestPriorityPresence?.status
//                val isOnline = !hightestPriorityPresence.isAway
//
//                if (userId != null) {
//                    launch {
//                        send(
//                            PresenceUpdate(
//                                userId = userId,
//                                presence = RemotePresence(
//                                    isOnline = isOnline,
//                                    status = status,
//                                    lastOnline = null
//                                )
//                            )
//                        )
//
//                    }
//                }
//            }
//
//            override fun presenceError(address: Jid?, errorPresence: Presence?) {}
//            override fun presenceSubscribed(address: BareJid?, subscribedPresence: Presence?) {}
//            override fun presenceUnsubscribed(address: BareJid?, unsubscribedPresence: Presence?) {}
//
//        }
//
//        rosterManager.addPresenceEventListener(listener)
//
//        awaitClose { rosterManager.removePresenceEventListener(listener) }
//    }

    override suspend fun fetchAvatarData(userId: String, hash: String): CallResult<String?> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val pubSubManager =
                    PubSubManager.getInstanceFor(connection, JidCreate.bareFrom(userId))
                val avatarNode = pubSubManager.getLeafNode(AvatarDataExtensionElement.NAMESPACE)
                val items =
                    avatarNode.getItems<PayloadItem<AvatarDataExtensionElement>>(listOf(hash))
                CallResult.Success(items.first().payload.data)
            } catch (e: Exception) {
                CallResult.Error("Error while retrieving avatar data")
            }
        }

    override suspend fun blockUser(userId: String): CallResult<Boolean> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                blockingCommandManager.blockContacts(listOf(JidCreate.bareFrom(userId)))
                CallResult.Success(true)
            } catch (e: Exception) {
                Logger.e(e)
                CallResult.Error("Failed to block user", e)
            }
        }

    override suspend fun unblockUser(userId: String): CallResult<Boolean> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                blockingCommandManager.unblockContacts(listOf(JidCreate.bareFrom(userId)))
                CallResult.Success(true)
            } catch (e: Exception) {
                Logger.e(e)
                CallResult.Error("Failed to block user", e)
            }
        }

    private suspend fun getUserVCard(userId: String): org.jivesoftware.smackx.vcardtemp.packet.VCard? =
        withContext(Dispatchers.IO) {
            return@withContext try {
                vCardManager.loadVCard(JidCreate.entityBareFrom(userId))
            } catch (e: NoResponseException) {
                Timber.e(e)
                null
            } catch (e: XMPPException) {
                Timber.e(e)
                null
            } catch (e: NotConnectedException) {
                Timber.e(e)
                null
            } catch (e: InterruptedException) {
                Timber.e(e)
                null
            }
        }

    private suspend fun getUserPresence(userId: String): Presence? = withContext(Dispatchers.IO) {
        return@withContext try {
            rosterManager.getPresence(JidCreate.entityBareFrom(userId))
        } catch (e: NoResponseException) {
            Timber.e(e)
            null
        } catch (e: XMPPException) {
            Timber.e(e)
            null
        } catch (e: NotConnectedException) {
            Timber.e(e)
            null
        } catch (e: InterruptedException) {
            Timber.e(e)
            null
        }
    }

    fun listenForUserChanges() = callbackFlow {

        val rosterListener = object : RosterListener {

            override fun entriesAdded(addresses: MutableCollection<Jid>?) {
                Timber.d("Roster entries added")
                addresses?.forEach { jid ->

                    launch {
                        val remoteUser = getUpdatedUserInfo(jid.asBareJid().toString())

                        if (remoteUser is CallResult.Error) {
                            Timber.e("Failed to fetch user info for ${jid.asBareJid()}")
                        }
                        val fetchedUser = (remoteUser as CallResult.Success<RemoteUser>).data

                        send(
                            ListChange.ItemAdded(
                                RemoteUser(
                                    id = jid.asBareJid().toString(),
                                    name = fetchedUser?.name,
                                    avatar = fetchedUser?.avatar
                                )
                            )
                        )
                    }
                }
            }

            override fun entriesUpdated(addresses: MutableCollection<Jid>?) {
                Timber.d("Roster entries updated")
                addresses?.forEach { jid ->

                    launch {
                        val remoteUser = getUpdatedUserInfo(jid.asBareJid().toString())

                        if (remoteUser is CallResult.Error) {
                            Timber.e("Failed to fetch user info for ${jid.asBareJid()}")
                        }
                        val fetchedUser = (remoteUser as CallResult.Success<RemoteUser>).data

                        send(
                            ListChange.ItemUpdated(
                                RemoteUser(
                                    id = jid.asBareJid().toString(),
                                    name = fetchedUser?.name,
                                    avatar = fetchedUser?.avatar
                                )
                            )
                        )
                    }
                }
            }

            override fun entriesDeleted(addresses: MutableCollection<Jid>?) {
                TODO("Not yet implemented")
            }

            override fun presenceChanged(presence: Presence) {
                val from = presence.from

                from?.let { jid ->
                    val bestPresence = rosterManager.getPresence(jid.asBareJid())
                    val isOnline = bestPresence.isAvailable

                    launch {
                        try {
                            if (connection.isAuthenticated) {
                                val lastOnline =
                                    if (isOnline) null else lastActivity(
                                        from.asBareJid().toString()
                                    )
                                val lastOnlineDateTime = lastOnline?.let {
                                    Instant.ofEpochMilli(it).atOffset(
                                        ZoneOffset.UTC
                                    )
                                }
                                send(
                                    ListChange.ItemUpdated(
                                        RemotePresence(
                                            isOnline = isOnline,
                                            lastOnline = lastOnlineDateTime,
                                            status = bestPresence.status,
                                        )
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            Timber.e(e)
                        }
                    }
                }
            }
        }

        rosterManager.addRosterListener(rosterListener)

        awaitClose {
            rosterManager.removeRosterListener(rosterListener)
        }
    }

    suspend fun lastActivity(userId: String) = withContext(Dispatchers.IO) {
        return@withContext try {
            val lastActivity = lastActivityManager.getLastActivity(userId.asJid())
            lastActivity.lastActivity
        } catch (e: XMPPException) {
            Timber.e(e)
            null
        } catch (e: NoResponseException) {
            Timber.e(e)
            null
        } catch (e: NotConnectedException) {
            Timber.e(e)
            null
        } catch (e: InterruptedException) {
            Timber.e(e)
            null
        }
    }

//    suspend fun getParticipantsInfo(conversation: String) = withContext(Dispatchers.IO) {
//        return@withContext mucChatManager
//            .getMultiUserChat(JidCreate.entityBareFrom(conversation))
//            .participants
//            .map {
//                getUpdatedUserInfo(
//                    it.jid.asBareJid().toString()
//                )
//            }
//    }
}