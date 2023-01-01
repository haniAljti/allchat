package com.hanialjti.allchat.data.remote.xmpp

import com.hanialjti.allchat.common.model.ListChange
import com.hanialjti.allchat.data.remote.UserRemoteDataSource
import com.hanialjti.allchat.data.remote.model.CallResult
import com.hanialjti.allchat.data.remote.model.RemoteUser
import com.hanialjti.allchat.data.remote.model.RemoteUserItem
import com.hanialjti.allchat.presentation.chat.defaultName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jivesoftware.smack.SmackException
import org.jivesoftware.smack.XMPPException
import org.jivesoftware.smack.packet.Presence
import org.jivesoftware.smack.roster.Roster
import org.jivesoftware.smack.roster.RosterListener
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smackx.iqlast.LastActivityManager
import org.jivesoftware.smackx.vcardtemp.VCardManager
import org.jxmpp.jid.Jid
import org.jxmpp.jid.impl.JidCreate
import timber.log.Timber
import java.time.Instant
import java.time.ZoneOffset

class XmppUserRemoteDataSource(
    private val connection: XMPPTCPConnection
) : UserRemoteDataSource {

    private val vCardManager = VCardManager.getInstanceFor(connection)
    private val rosterManager = Roster.getInstanceFor(connection)
    private val lastAccountManager = LastActivityManager.getInstanceFor(connection)

    override suspend fun getUpdatedUserInfo(userId: String) = withContext(Dispatchers.IO) {
        return@withContext try {
            vCardManager.loadVCard(JidCreate.entityBareFrom(userId)).let { vCard ->
                CallResult.Success(
                    RemoteUser(
                        id = userId,
                        name = vCard?.firstName ?: defaultName,
                        image = vCard?.avatar?.toString(),
                    )
                )
            }
        } catch (e: Exception) {
            Timber.e(e)
            CallResult.Error("An error occurred while fetching user info", e)
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
                                RemoteUserItem.UserData(
                                    id = jid.asBareJid().toString(),
                                    name = fetchedUser?.name,
                                    image = fetchedUser?.image
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
                                RemoteUserItem.UserData(
                                    id = jid.asBareJid().toString(),
                                    name = fetchedUser?.name,
                                    image = fetchedUser?.image
                                )
                            )
                        )
                    }
                }
            }

            override fun entriesDeleted(addresses: MutableCollection<Jid>?) {
                TODO("Not yet implemented")
            }

            override fun presenceChanged(presence: Presence?) {
                val from = presence?.from

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
                                        RemoteUserItem.UserPresence(
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