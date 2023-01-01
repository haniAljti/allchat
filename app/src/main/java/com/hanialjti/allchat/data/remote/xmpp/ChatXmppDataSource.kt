package com.hanialjti.allchat.data.remote.xmpp

import com.hanialjti.allchat.common.model.ListChange
import com.hanialjti.allchat.data.remote.ChatRemoteDataSource
import com.hanialjti.allchat.data.remote.UserRemoteDataSource
import com.hanialjti.allchat.data.remote.model.CallResult
import com.hanialjti.allchat.data.remote.model.RemoteChat
import com.hanialjti.allchat.data.remote.model.RemoteUser
import com.hanialjti.allchat.data.remote.model.RemoteUserItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jivesoftware.smack.SmackException.NoResponseException
import org.jivesoftware.smack.SmackException.NotConnectedException
import org.jivesoftware.smack.StanzaListener
import org.jivesoftware.smack.XMPPException
import org.jivesoftware.smack.filter.*
import org.jivesoftware.smack.packet.Presence
import org.jivesoftware.smack.roster.Roster
import org.jivesoftware.smack.roster.RosterListener
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smackx.bookmarks.BookmarkManager
import org.jivesoftware.smackx.iqlast.LastActivityManager
import org.jivesoftware.smackx.muc.Affiliate
import org.jivesoftware.smackx.muc.MultiUserChatManager
import org.jivesoftware.smackx.muc.RoomInfo
import org.jivesoftware.smackx.muc.packet.GroupChatInvitation
import org.jivesoftware.smackx.pep.PepManager
import org.jivesoftware.smackx.pubsub.PubSubManager
import org.jivesoftware.smackx.pubsub.packet.PubSub
import org.jxmpp.jid.Jid
import org.jxmpp.jid.impl.JidCreate
import org.jxmpp.jid.parts.Resourcepart
import timber.log.Timber
import java.util.*

class ChatXmppDataSource(
    private val connection: XMPPTCPConnection,
    private val bookmarkManager: BookmarkManager,
    private val rosterManager: Roster,
    private val userXmppDataSource: UserRemoteDataSource
) : ChatRemoteDataSource {

    private val lastOnline = LastActivityManager.getInstanceFor(connection)
    private val mucManager = MultiUserChatManager.getInstanceFor(connection)
    private val pep = PepManager.getInstanceFor(connection)

    init {
//        val pubSub = PubSubManager.getInstanceFor(connection, JidCreate.entityBareFrom("2aa66faa-dd15-4229-b418-22b73b6649e6@conference.localhost"))
//        pubSub.getNode("").subscribe(connection.user.asBareJid())
    }

    override suspend fun retrieveContacts() = withContext(Dispatchers.IO) {
        val conferences = try {
            bookmarkManager
                .bookmarkedConferences
                .map {
                    RemoteChat(
                        id = it.jid.asEntityBareJidString(),
                        name = it.name,
                        isGroupChat = true
                    )
                }
        } catch (e: Exception) {
            Timber.e(e)
            listOf()
        }


        val users = try {
            rosterManager.entries
                .map {
                    val remoteUser =
                        userXmppDataSource.getUpdatedUserInfo(it.jid.asBareJid().toString())
                    if (remoteUser is CallResult.Error) {
                        Timber.e("Failed to fetch user info for ${it.jid.asBareJid()}")
                    }
                    RemoteChat(
                        id = it.jid.asBareJid().toString(),
                        name = it.name,
                        image = (remoteUser as? CallResult.Success<RemoteUser>)?.data?.image,
                        isGroupChat = false
                    )
                }
        } catch (e: Exception) {
            Timber.e(e)
            listOf()
        }

        return@withContext conferences.plus(users)
    }


    override suspend fun addUserToContact(userId: String, userName: String): CallResult<String> {
        return try {
            rosterManager.createItemAndRequestSubscription(
                userId.asJid(),
                userName,
                null
            )
            CallResult.Success(userId)
        } catch (e: Exception) {
            CallResult.Error("An error occurred while adding user to roster", e)
        }
    }


    override fun listenForChatChanges() = callbackFlow {

        val rosterListener = object : RosterListener {

            override fun entriesAdded(addresses: MutableCollection<Jid>?) {
                addresses?.forEach { jid ->

                    launch {
                        val remoteUser =
                            userXmppDataSource.getUpdatedUserInfo(jid.asBareJid().toString())

                        if (remoteUser is CallResult.Error) {
                            Timber.e("Failed to fetch user info for ${jid.asBareJid()}")
                        }
                        val fetchedUser = (remoteUser as CallResult.Success<RemoteUser>).data

                        send(
                            RemoteChat(
                                id = jid.asBareJid().toString(),
                                name = fetchedUser?.name,
                                image = fetchedUser?.image,
                                isGroupChat = false
                            )
                        )
                    }
                }
            }

            override fun entriesUpdated(addresses: MutableCollection<Jid>?) {
                Timber.d("Roster entries updated")
                addresses?.forEach { jid ->

                    launch {
                        val remoteUser =
                            userXmppDataSource.getUpdatedUserInfo(jid.asBareJid().toString())

                        if (remoteUser is CallResult.Error) {
                            Timber.e("Failed to fetch user info for ${jid.asBareJid()}")
                        }
                        val fetchedUser = (remoteUser as CallResult.Success<RemoteUser>).data

                        send(
                            RemoteChat(
                                id = jid.asBareJid().toString(),
                                name = fetchedUser?.name,
                                image = fetchedUser?.image,
                                isGroupChat = false
                            )
                        )
                    }
                }
            }

            override fun entriesDeleted(addresses: MutableCollection<Jid>?) {}
            override fun presenceChanged(presence: Presence?) {}
        }

        rosterManager.addRosterListener(rosterListener)

        awaitClose {
            rosterManager.removeRosterListener(rosterListener)
        }
    }

    suspend fun lastActivity(userId: String) = withContext(Dispatchers.IO) {
        return@withContext try {
            val lastActivity = lastOnline.getLastActivity(userId.asJid())
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


    override suspend fun createChatRoom(
        roomName: String,
        myId: String
    ): CallResult<String> {
        return try {
            val roomJid =
                "${UUID.randomUUID()}@${mucManager.mucServiceDomains.first()}"
            val muc = mucManager.getMultiUserChat(roomJid.asJid().asEntityBareJidIfPossible())

            muc.create(Resourcepart.from(myId))
                .configFormManager
                .makeMembersOnly()
                .submitConfigurationForm()

            bookmarkConference(roomJid, roomName)
            joinRoom(roomJid, myId)
            CallResult.Success(roomJid)
        } catch (e: Exception) {
            Timber.e(e)
            CallResult.Error("An error occurred while creating chat room", e)
        }
    }


    private fun listenForGroupInvitations() = callbackFlow<ListChange<RemoteUserItem>> {

        val invitationFilter: StanzaFilter = AndFilter(
            StanzaTypeFilter.MESSAGE,
            StanzaExtensionFilter(GroupChatInvitation.NAMESPACE),
            NotFilter(MessageTypeFilter.ERROR)
        )
        val invitationListener = StanzaListener { message ->
            Timber.d("New Group Invitation: message:$message")
            val invitation =
                message.getExtension(GroupChatInvitation.NAMESPACE) as? GroupChatInvitation
            invitation?.roomAddress?.let { roomAddress ->
                launch {
                    val roomInfo = mucManager.getRoomInfo(
                        invitation.roomAddress.asJid().asEntityBareJidIfPossible()
                    )
                    addGroupChatToContacts(roomAddress, roomInfo.name)
//                    send(
//                        RemoteChat(
//                            roomAddress,
//                            roomInfo.name,
//
//                        )
//                    )
                }
            }
        }
        connection.addStanzaListener(invitationListener, invitationFilter)
        awaitClose { connection.removeStanzaListener(invitationListener) }
    }

    override suspend fun inviteUserToChatRoom(
        userId: String,
        conversationId: String,
        myId: String
    ): CallResult<String> {
        return try {
            joinRoom(conversationId, myId)
            mucManager.getMultiUserChat(conversationId.asJid().asEntityBareJidIfPossible())
                .inviteDirectly(userId.asJid().asEntityBareJidIfPossible())
            CallResult.Success(userId)
        } catch (e: Exception) {
            Timber.e(e)
            CallResult.Error("Could not invite user $userId", e)
        }
    }

//    suspend fun listenForGroupChatInvitations() = callbackFlow {
//        val invitationListener = InvitationListener { _, room, inviter, reason, pass, message, invitation ->
//            bookmarkConference(
//                conversationId = room.room.asBareJid().toString(),
//                conversationName = room.
//            )
//        }
//
//        mucManager.addInvitationListener(invitationListener)
//        mucManager.decline()
//        awaitClose {
//            mucManager.removeInvitationListener(invitationListener)
//        }
//    }

    suspend fun getRoomInfo(roomId: String, myNickName: String): CallResult<RoomInfo> {
        return try {
            joinRoom(roomId, myNickName)
            CallResult.Success(mucManager.getRoomInfo(roomId.asJid().asEntityBareJidIfPossible()))
        } catch (e: NoResponseException) {
            CallResult.Error(message = "No response from server", cause = e)
        } catch (e: NotConnectedException) {
            CallResult.Error(message = "Not connected", cause = e)
        } catch (e: XMPPException.XMPPErrorException) {
            CallResult.Error(message = "Xmpp error", cause = e)
        } catch (e: InterruptedException) {
            CallResult.Error(message = "Request interrupted", cause = e)
        }
    }

    suspend fun getRoomMembers(roomId: String, myNickName: String): CallResult<List<Affiliate>> {
        return try {
            joinRoom(roomId, myNickName)
            CallResult.Success(
                mucManager.getMultiUserChat(
                    roomId.asJid().asEntityBareJidIfPossible()
                ).members
            )
        } catch (e: NoResponseException) {
            CallResult.Error(message = "No response from server", cause = e)
        } catch (e: NotConnectedException) {
            CallResult.Error(message = "Not connected", cause = e)
        } catch (e: XMPPException.XMPPErrorException) {
            CallResult.Error(message = "Xmpp error", cause = e)
        } catch (e: InterruptedException) {
            CallResult.Error(message = "Request interrupted", cause = e)
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

    suspend fun addGroupChatToContacts(
        roomId: String,
        roomName: String
    ): CallResult<String> {
        return try {
            bookmarkConference(roomId, roomName)
            CallResult.Success(roomId)
        } catch (e: Exception) {
            CallResult.Error("An error occurred while creating chat room", e)
        }
    }

    private suspend fun bookmarkConference(
        chatRoomId: String,
        chatRoomName: String
    ) {
        bookmarkManager.addBookmarkedConference(
            chatRoomName,
            chatRoomId.asJid().asEntityBareJidIfPossible(),
            true,
            null,
            null
        )
    }

}