package com.hanialjti.allchat.data.remote.xmpp

import com.hanialjti.allchat.common.model.ListChange
import com.hanialjti.allchat.common.utils.Logger
import com.hanialjti.allchat.data.model.ChatState
import com.hanialjti.allchat.data.remote.ChatRemoteDataSource
import com.hanialjti.allchat.data.remote.model.*
import com.hanialjti.allchat.data.remote.xmpp.model.MucBookmark
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.merge
import org.jivesoftware.smack.SmackException.NoResponseException
import org.jivesoftware.smack.SmackException.NotConnectedException
import org.jivesoftware.smack.StanzaListener
import org.jivesoftware.smack.XMPPException.XMPPErrorException
import org.jivesoftware.smack.chat2.ChatManager
import org.jivesoftware.smack.filter.*
import org.jivesoftware.smack.packet.Presence
import org.jivesoftware.smack.roster.Roster
import org.jivesoftware.smack.roster.RosterListener
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smackx.bookmarks.BookmarkManager
import org.jivesoftware.smackx.chatstates.ChatStateListener
import org.jivesoftware.smackx.chatstates.ChatStateManager
import org.jivesoftware.smackx.muc.Affiliate
import org.jivesoftware.smackx.muc.MultiUserChatManager
import org.jivesoftware.smackx.muc.RoomInfo
import org.jivesoftware.smackx.muc.packet.GroupChatInvitation
import org.jivesoftware.smackx.nick.packet.Nick
import org.jivesoftware.smackx.pep.PepManager
import org.jivesoftware.smackx.pubsub.*
import org.jivesoftware.smackx.vcardtemp.VCardManager
import org.jxmpp.jid.Jid
import org.jxmpp.jid.impl.JidCreate
import org.jxmpp.jid.parts.Resourcepart
import timber.log.Timber

class ChatXmppDataSource(
    private val connection: XMPPTCPConnection,
    private val roster: Roster,
    private val dispatcher: CoroutineDispatcher,
    private val externalScope: CoroutineScope,
    private val localMucManager: MucManager,
    private val mucManager: MultiUserChatManager = MultiUserChatManager.getInstanceFor(connection),
    private val bookmarkManager: BookmarkManager = BookmarkManager.getBookmarkManager(connection),
    private val chatManager: ChatManager = ChatManager.getInstanceFor(connection),
    private val chatStateManager: ChatStateManager = ChatStateManager.getInstance(connection),
    private val vCardManager: VCardManager = VCardManager.getInstanceFor(connection),
    private val pubSubManager: PubSubManager = PubSubManager.getInstanceFor(connection),
    private val pepManager: PepManager = PepManager.getInstanceFor(connection)
) : ChatRemoteDataSource {

    // TODO Support XEP-0048
    override suspend fun retrieveGroupChats() = withContext(dispatcher) {
        val conferences = try {
            bookmarkManager
                .bookmarkedConferences
                .map {
                    it.jid.asEntityBareJidString()
                }
        } catch (e: Exception) {
            Timber.e(e)
            listOf()
        }

        val conferencesPep = try {
            val pubSubManager =
                PubSubManager.getInstanceFor(connection, connection.user.asBareJid())
            pubSubManager
                .getLeafNode(MucBookmark.NAMESPACE)
                .getItems<PayloadItem<MucBookmark>>()
                .map { item ->
                    item.id
                }
        } catch (e: Exception) {
            Timber.e(e)
            listOf()
        }

        return@withContext conferences.plus(conferencesPep)
    }


    override suspend fun addUserToContact(userId: String): CallResult<String> {
        return try {
            roster.createItem(userId.asJid(), null, null)
            sendPresenceSubscription(userId)
            CallResult.Success(userId)
        } catch (e: Exception) {
            CallResult.Error("An error occurred while adding user to roster", e)
        }
    }

    private suspend fun sendPresenceSubscription(to: String) {
        val nickname = connection.user?.asBareJid()?.toString()?.let { fetchNickname(it) }
        val presencePacket = connection.stanzaFactory.buildPresenceStanza()
            .ofType(Presence.Type.subscribe)
            .to(to.asJid())
            .apply {
                if (nickname != null && nickname is CallResult.Success) {
                    addExtension(
                        Nick(nickname.data)
                    )
                }
            }
            .build()
        connection.sendStanza(presencePacket)
    }

    private suspend fun fetchNickname(id: String): CallResult<String?> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                if (pepManager.isSupported) {
                    val pubSubManager =
                        PubSubManager.getInstanceFor(connection, JidCreate.bareFrom(id))
                    val nicknameNode = pubSubManager.getLeafNode(Nick.NAMESPACE)
                    val items = nicknameNode.getItems<PayloadItem<Nick>>()
                    CallResult.Success(items.first().payload.name)
                } else {
                    val vCard = vCardManager.loadVCard()
                    CallResult.Success(vCard.nickName)
                }
            } catch (e: Exception) {
                CallResult.Error("Error while retrieving nickname")
            }
        }

    override suspend fun createChatRoom(
        roomName: String,
        myId: String,
        invitees: Set<String>
    ): CallResult<String> = localMucManager.createChatRoom(roomName, myId, invitees)

    @kotlin.jvm.Throws(
        XMPPErrorException::class,
        NoResponseException::class,
        NotConnectedException::class,
        InterruptedException::class
    )
    private fun createPubSubNodeForRoom(roomId: String): String {
        val node = pubSubManager.getOrCreateLeafNode(roomId)

        val configureForm = node
            .nodeConfiguration
            .fillableForm
            .apply {
                accessModel = AccessModel.whitelist
                publishModel = PublishModel.subscribers
                isDeliverPayloads = true
            }
        node.sendConfigurationForm(configureForm)
        return "xmpp:${node.id}@${pubSubManager.serviceJid}"
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
            val pubSubNode =
                mucManager.getRoomInfo(conversationId.asJid().asEntityBareJidIfPossible())
                    .pubSub
            pubSubManager.getLeafNode(pubSubNode).modifyAffiliationAsOwner(
                listOf(Affiliation(userId.asJid(), Affiliation.Type.publisher))
            )
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
        } catch (e: XMPPErrorException) {
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
        } catch (e: XMPPErrorException) {
            CallResult.Error(message = "Xmpp error", cause = e)
        } catch (e: InterruptedException) {
            CallResult.Error(message = "Request interrupted", cause = e)
        }
    }

    private fun listenForChatStateUpdates() = callbackFlow {
        val chatStateListener = ChatStateListener { _, state, message ->
            val conversation = message.from.asBareJid().toString()
            val from = message.from.localpartOrNull?.toString()

            from?.let {
                launch {
                    send(
                        ChatStateUpdate(state.toConversationState(conversation, it))
                    )
                }
            }

        }

        chatStateManager.addChatStateListener(chatStateListener)

        awaitClose { chatStateManager.removeChatStateListener(chatStateListener) }
    }

    private fun listenForNewContacts() = callbackFlow {

        val entries = roster.entries
        entries.forEach { rosterEntry ->
            trySend(
                NewContactUpdate(
                    rosterEntry.jid.asBareJid().toString(),
                    isGroupChat = false
                )
            )
        }

        val rosterListener = object : RosterListener {

            override fun entriesAdded(addresses: MutableCollection<Jid>) {
                Logger.d {
                    "New roster entries are received with jids: ${
                        addresses.map {
                            it.asBareJid().toString()
                        }
                    }"
                }
                addresses.forEach { jid ->
                    trySend(
                        NewContactUpdate(
                            jid.asBareJid().toString(),
                            isGroupChat = false
                        )
                    )
                }
            }

            override fun entriesUpdated(addresses: MutableCollection<Jid>) {
                Logger.d {
                    "Updated roster entries are received with jids: ${
                        addresses.map {
                            it.asBareJid().toString()
                        }
                    }"
                }
                addresses.forEach { jid ->
                    trySend(
                        NewContactUpdate(
                            jid.asBareJid().toString(),
                            isGroupChat = false
                        )
                    )
                }

            }

            override fun entriesDeleted(addresses: MutableCollection<Jid>?) {
                Logger.d {
                    "Deleted roster entries are received with jids: ${
                        addresses?.map {
                            it.asBareJid().toString()
                        }
                    }"
                }
            }

            override fun presenceChanged(presence: Presence?) {}
        }

        roster.addRosterListener(rosterListener)
        awaitClose { roster.removeRosterListener(rosterListener) }
    }

    override fun chatUpdatesStream(): Flow<ChatUpdate> = merge(
        listenForChatStateUpdates(),
        listenForNewContacts(),
        localMucManager.bookmarkedConferencesStream()
    )

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

    private suspend fun addGroupChatToContacts(
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
        try {
            if (pepManager.isSupported) {
                val pubSub = pepManager.pepPubSubManager
                val bookmarksNode = pubSub.getOrCreateLeafNode(MucBookmark.NAMESPACE)

                val configureForm = bookmarksNode
                    .nodeConfiguration
                    .fillableForm
                    .apply {
                        setPersistentItems(true)
                        accessModel = AccessModel.whitelist
                        setAnswer("pubsub#send_last_published_item", "never")
                        maxItems = 1000
                    }
                bookmarksNode.sendConfigurationForm(configureForm)
                pepManager.publish(
                    MucBookmark.NAMESPACE,
                    PayloadItem(chatRoomId, MucBookmark())
                )
            } else {
                bookmarkManager.addBookmarkedConference(
                    chatRoomName,
                    chatRoomId.asJid().asEntityBareJidIfPossible(),
                    true,
                    null,
                    null
                )
            }
        } catch (e: Exception) {
            Logger.e(e)

        }
    }

}