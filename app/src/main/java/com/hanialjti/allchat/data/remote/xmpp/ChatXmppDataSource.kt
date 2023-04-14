package com.hanialjti.allchat.data.remote.xmpp

import com.hanialjti.allchat.common.utils.Logger
import com.hanialjti.allchat.data.model.ChatState
import com.hanialjti.allchat.data.model.Participant
import com.hanialjti.allchat.data.remote.ChatRemoteDataSource
import com.hanialjti.allchat.data.remote.model.*
import com.hanialjti.allchat.data.remote.xmpp.model.ItemAdded
import com.hanialjti.allchat.data.remote.xmpp.model.ItemDeleted
import com.hanialjti.allchat.data.remote.xmpp.model.ItemUpdated
import com.hanialjti.allchat.data.remote.xmpp.model.MucStateUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jivesoftware.smack.chat2.ChatManager
import org.jivesoftware.smack.packet.Presence
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smackx.chatstates.ChatStateListener
import org.jivesoftware.smackx.chatstates.ChatStateManager
import org.jivesoftware.smackx.nick.packet.Nick
import org.jivesoftware.smackx.pep.PepManager
import org.jivesoftware.smackx.pubsub.PayloadItem
import org.jivesoftware.smackx.pubsub.PubSubManager
import org.jivesoftware.smackx.vcardtemp.VCardManager
import org.jxmpp.jid.impl.JidCreate
import timber.log.Timber

class ChatXmppDataSource(
    private val connection: XMPPTCPConnection,
    private val rosterManager: RosterManager,
    private val mucManager: MucManager,
    private val chatManager: ChatManager = ChatManager.getInstanceFor(connection),
    private val chatStateManager: ChatStateManager = ChatStateManager.getInstance(connection),
    private val vCardManager: VCardManager = VCardManager.getInstanceFor(connection),
    private val pepManager: PepManager = PepManager.getInstanceFor(connection)
) : ChatRemoteDataSource {

    override suspend fun addUserToContact(userId: String): CallResult<String> {
        return try {
            rosterManager.createRosterItemAndSendSubscriptionRequest(userId)
            CallResult.Success(userId)
        } catch (e: Exception) {
            CallResult.Error("An error occurred while adding user to roster", e)
        }
    }

    override suspend fun updateChatInfo(
        chatId: String,
        description: String?,
        avatarUrl: String?,
        subject: String?
    ) = mucManager.updateChatInfo(
        chatId, description, avatarUrl, subject
    )


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
        invitees: Set<String>
    ): CallResult<String> = mucManager.createChatRoom(roomName, invitees)

    private fun participantStateStream() = callbackFlow {

        val listener = ChatStateListener { chat, state, _ ->
            launch {
                send(
                    ParticipantStateUpdated(
                        chat.xmppAddressOfChatPartner.asBareJid().toString(),
                        chat.xmppAddressOfChatPartner.asBareJid().toString(),
                        when (state) {
                            org.jivesoftware.smackx.chatstates.ChatState.composing -> Participant.State.Composing
                            else -> Participant.State.Paused
                        }
                    )
                )
            }
        }

        chatStateManager.addChatStateListener(listener)

        awaitClose { chatStateManager.removeChatStateListener(listener) }
    }

    override suspend fun updateMyChatState(chatId: String, state: Participant.State) {
        val smackState = when (state) {
            Participant.State.Composing -> org.jivesoftware.smackx.chatstates.ChatState.composing
            else -> org.jivesoftware.smackx.chatstates.ChatState.paused
        }

        val chat = ChatManager.getInstanceFor(connection)
            .chatWith(chatId.asJid().asEntityBareJidIfPossible())

        try {
            chatStateManager.setCurrentState(smackState, chat)
        } catch (e: Exception) {
            Logger.e(e)
        }
    }

    override suspend fun inviteUserToChatRoom(
        userId: String,
        conversationId: String,
        myId: String
    ): CallResult<String> {
//        return try {
//            localMucManager.joinRoom(conversationId)
//            val pubSubNode =
//                mucManager.getRoomInfo(conversationId.asJid().asEntityBareJidIfPossible())
//                    .pubSub
//            pubSubManager.getLeafNode(pubSubNode).modifyAffiliationAsOwner(
//                listOf(Affiliation(userId.asJid(), Affiliation.Type.publisher))
//            )
//            mucManager.getMultiUserChat(conversationId.asJid().asEntityBareJidIfPossible())
//                .inviteDirectly(userId.asJid().asEntityBareJidIfPossible())
//            CallResult.Success(userId)
//        } catch (e: Exception) {
//            Timber.e(e)
        return CallResult.Error("Could not invite user $userId")
//        }
    }

    override suspend fun getRoomInfo(roomId: String): CallResult<RemoteRoomInfo> =
        mucManager.getRoomInfo(roomId)

    override suspend fun getRoomSubject(roomId: String): CallResult<String> {
        return try {
            CallResult.Success(mucManager.getRoomSubject(roomId))
        } catch (e: Exception) {
            Logger.e(e)
            CallResult.Error("An error occurred while fetching room subject")
        }
    }

    override fun chatUpdatesStream(): Flow<ChatUpdate> = merge(
        rosterManager.rosterUpdateStream
            .onEach { Logger.d { "New roster update" } }
//            .filter { it is ItemAdded || it is ItemUpdated || it is ItemDeleted }
//            .onEach { Logger.d { "New roster item update" } }
            .mapNotNull {
                Logger.d { "New roster item update: $it" }
                when (it) {
                    is ItemAdded -> it.toChatUpdate()
                    is ItemDeleted -> it.toChatUpdate()
                    is ItemUpdated -> it.toChatUpdate()
                    else -> null
                }
            },
        mucManager.roomAdditionStream
            .map { it.toChatUpdate() },
        mucManager
            .roomChangesStream
            .filterIsInstance<MucStateUpdate>()
            .map { it.toChatUpdate() },
        participantStateStream()
    )


    override suspend fun updateMyChatState(chatState: ChatState) {
        val smackChatState = when (chatState) {
            is ChatState.Composing -> org.jivesoftware.smackx.chatstates.ChatState.composing
            is ChatState.Active -> org.jivesoftware.smackx.chatstates.ChatState.active
            is ChatState.Paused -> org.jivesoftware.smackx.chatstates.ChatState.paused
            else -> org.jivesoftware.smackx.chatstates.ChatState.inactive
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

}