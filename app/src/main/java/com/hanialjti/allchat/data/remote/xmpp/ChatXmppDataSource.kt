package com.hanialjti.allchat.data.remote.xmpp

import com.hanialjti.allchat.common.model.ListChange
import com.hanialjti.allchat.data.model.ChatState
import com.hanialjti.allchat.data.model.Avatar
import com.hanialjti.allchat.data.remote.ChatRemoteDataSource
import com.hanialjti.allchat.data.remote.UserRemoteDataSource
import com.hanialjti.allchat.data.remote.model.CallResult
import com.hanialjti.allchat.data.remote.model.RemoteChat
import com.hanialjti.allchat.data.remote.model.RemoteUserItem
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import org.jivesoftware.smack.SmackException.NoResponseException
import org.jivesoftware.smack.SmackException.NotConnectedException
import org.jivesoftware.smack.StanzaListener
import org.jivesoftware.smack.XMPPException
import org.jivesoftware.smack.chat2.ChatManager
import org.jivesoftware.smack.filter.*
import org.jivesoftware.smack.packet.Presence
import org.jivesoftware.smack.roster.Roster
import org.jivesoftware.smack.roster.RosterListener
import org.jivesoftware.smack.roster.rosterstore.RosterStore
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smackx.bookmarks.BookmarkManager
import org.jivesoftware.smackx.chatstates.ChatStateListener
import org.jivesoftware.smackx.chatstates.ChatStateManager
import org.jivesoftware.smackx.iqlast.LastActivityManager
import org.jivesoftware.smackx.muc.Affiliate
import org.jivesoftware.smackx.muc.MultiUserChatManager
import org.jivesoftware.smackx.muc.RoomInfo
import org.jivesoftware.smackx.muc.packet.GroupChatInvitation
import org.jivesoftware.smackx.pep.PepManager
import org.jivesoftware.smackx.vcardtemp.VCardManager
import org.jxmpp.jid.Jid
import org.jxmpp.jid.impl.JidCreate
import org.jxmpp.jid.parts.Resourcepart
import timber.log.Timber
import java.util.*

class ChatXmppDataSource(
    private val connection: XMPPTCPConnection,
    private val bookmarkManager: BookmarkManager,
    private val rosterStore: RosterStore,
    private val roster: Roster,
    private val userXmppDataSource: UserRemoteDataSource
) : ChatRemoteDataSource {

    private val _chatChanges = MutableSharedFlow<ListChange<RemoteChat>>()
    override val chatChanges = _chatChanges.asSharedFlow()

    private val lastOnline = LastActivityManager.getInstanceFor(connection)
    private val mucManager = MultiUserChatManager.getInstanceFor(connection)
    private val chatManager = ChatManager.getInstanceFor(connection)
    private val chatStateManager = ChatStateManager.getInstance(connection)
    private val vCardManager = VCardManager.getInstanceFor(connection)
    private val pep = PepManager.getInstanceFor(connection)

    init {

//        val pubSub = PubSubManager.getInstanceFor(connection, JidCreate.entityBareFrom("2aa66faa-dd15-4229-b418-22b73b6649e6@conference.localhost"))
//        pubSub.getNode("").subscribe(connection.user.asBareJid())
    }

    private val rosterListener = object : RosterListener {

        override fun entriesAdded(addresses: MutableCollection<Jid>?) {

            addresses?.forEach { jid ->
                _chatChanges.tryEmit(
                    ListChange.ItemAdded(
                        RemoteChat(
                            id = jid.asBareJid().toString(),
                            name = null,
                            isGroupChat = false
                        )
                    )
                )
            }
        }

        override fun entriesUpdated(addresses: MutableCollection<Jid>?) {
            Timber.d("Roster entries updated")
            addresses?.forEach { jid ->
                _chatChanges.tryEmit(
                    ListChange.ItemUpdated(
                        RemoteChat(
                            id = jid.asBareJid().toString(),
                            name = null,
                            isGroupChat = false
                        )
                    )
                )
            }
        }

        override fun entriesDeleted(addresses: MutableCollection<Jid>?) {}
        override fun presenceChanged(presence: Presence?) {}
    }

    override fun startListeners() {
        roster.addRosterListener(rosterListener)
    }

    override fun stopListeners() {
        roster.removeRosterListener(rosterListener)
    }

    override suspend fun emitChatChange(change: ListChange<RemoteChat>) {
        _chatChanges.emit(change)
    }

    private suspend fun getUpdatedVCard(jid: String) = withContext(Dispatchers.IO) {
        return@withContext try {
            vCardManager.loadVCard(JidCreate.entityBareFrom(jid))
        } catch (e: Exception) {
            Timber.e(e)
            null
        }
    }

    override suspend fun retrieveContacts() = withContext(Dispatchers.IO) {
        val conferences = try {
            bookmarkManager
                .bookmarkedConferences
                .map {
                    val roomVCard = getUpdatedVCard(it.jid.asEntityBareJidString())

                    RemoteChat(
                        id = it.jid.asEntityBareJidString(),
                        name = roomVCard?.nickName,
                        avatar = roomVCard?.avatar?.let { data -> Avatar.Raw(data, roomVCard.avatarHash) },
                        isGroupChat = true
                    )
                }
        } catch (e: Exception) {
            Timber.e(e)
            listOf()
        }

        val users = rosterStore.entries
            ?.map {
                val vCard = getUpdatedVCard(it.jid.asBareJid().toString())

                RemoteChat(
                    id = it.jid.asBareJid().toString(),
                    name = vCard?.nickName,
                    avatar = vCard?.avatar?.let { data -> Avatar.Raw(data, vCard.avatarHash) },
                    isGroupChat = false
                )
            } ?: emptyList()

        return@withContext conferences.plus(users)
    }


    override suspend fun addUserToContact(userId: String, userName: String): CallResult<String> {
        return try {
            roster.createItemAndRequestSubscription(
                userId.asJid(),
                userName,
                null
            )
            CallResult.Success(userId)
        } catch (e: Exception) {
            CallResult.Error("An error occurred while adding user to roster", e)
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

    override fun listenForChatStateUpdates() = callbackFlow {
        val chatStateListener = ChatStateListener { _, state, message ->
            val conversation = message.from.asBareJid().toString()
            val from = message.from.localpartOrNull?.toString()

            from?.let {
                launch { send(state.toConversationState(conversation, it)) }
            }

        }

        chatStateManager.addChatStateListener(chatStateListener)

        awaitClose { chatStateManager.removeChatStateListener(chatStateListener) }
    }

    override suspend fun updateMyChatState(chatState: ChatState) {
        val smackChatState = when (chatState) {
            is ChatState.Composing -> org.jivesoftware.smackx.chatstates.ChatState.composing
            is ChatState.Active -> org.jivesoftware.smackx.chatstates.ChatState.active
            is ChatState.Paused -> org.jivesoftware.smackx.chatstates.ChatState.paused
            is ChatState.Inactive -> org.jivesoftware.smackx.chatstates.ChatState.inactive
        }
        try {
            chatStateManager.setCurrentState(
                smackChatState,
                chatManager.chatWith(chatState.conversation.asJid().asEntityBareJidIfPossible())
            )

        } catch (e: Exception) {
            Timber.e(e)
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