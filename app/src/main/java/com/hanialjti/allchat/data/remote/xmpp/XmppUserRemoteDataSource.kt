package com.hanialjti.allchat.data.remote.xmpp

import com.hanialjti.allchat.common.model.ListChange
import com.hanialjti.allchat.data.model.Avatar
import com.hanialjti.allchat.data.remote.UserRemoteDataSource
import com.hanialjti.allchat.data.remote.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jivesoftware.smack.SmackException.NoResponseException
import org.jivesoftware.smack.SmackException.NotConnectedException
import org.jivesoftware.smack.XMPPException
import org.jivesoftware.smack.chat2.ChatManager
import org.jivesoftware.smack.packet.Presence
import org.jivesoftware.smack.roster.Roster
import org.jivesoftware.smack.roster.RosterListener
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smackx.iqlast.LastActivityManager
import org.jivesoftware.smackx.nick.packet.Nick
import org.jivesoftware.smackx.pep.PepEventListener
import org.jivesoftware.smackx.pep.PepManager
import org.jivesoftware.smackx.pubsub.PayloadItem
import org.jivesoftware.smackx.vcardtemp.VCardManager
import org.jxmpp.jid.Jid
import org.jxmpp.jid.impl.JidCreate
import timber.log.Timber
import java.time.Instant
import java.time.ZoneOffset

class XmppUserRemoteDataSource(
    private val connection: XMPPTCPConnection
) : UserRemoteDataSource {

    private val chatManager = ChatManager.getInstanceFor(connection)
    private val vCardManager = VCardManager.getInstanceFor(connection)
    private val rosterManager = Roster.getInstanceFor(connection)
    private val lastAccountManager = LastActivityManager.getInstanceFor(connection)
    private val pepManager = PepManager.getInstanceFor(connection)

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
                            Avatar.Raw(data, vCard.avatarHash)
                        }
                    ),
                    presence = RemotePresence(
                        id = userId,
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

    override suspend fun updateUserInfo(username: String): CallResult<Boolean> {
        return try {
            pepManager.publish("http://jabber.org/protocol/nick", PayloadItem(Nick(username)))
            CallResult.Success(true)
        } catch (e: Exception) {
            Timber.e(e)
            CallResult.Error("Error")
        }
    }

    override suspend fun listenForUsernameUpdates() = callbackFlow<String> {
        val listener = PepEventListener<Nick> { from, nickname, id, message ->
            Timber.d("New username: ${nickname.name}")
        }

        pepManager.addPepEventListener(
            "http://jabber.org/protocol/nick",
            Nick::class.java,
            listener
        )

        awaitClose { pepManager.removePepEventListener(listener) }
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
                                            id = jid.asBareJid().toString(),
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
            val lastActivity = lastAccountManager.getLastActivity(userId.asJid())
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