package com.hanialjti.allchat.data.remote.xmpp

import com.hanialjti.allchat.common.utils.Logger
import com.hanialjti.allchat.data.model.MessageStatus
import com.hanialjti.allchat.data.model.MessageType
import com.hanialjti.allchat.data.remote.model.CallResult
import com.hanialjti.allchat.data.remote.model.NewContactUpdate
import com.hanialjti.allchat.data.remote.model.RemoteMessage
import com.hanialjti.allchat.data.remote.xmpp.model.ChatMarkerWrapper.Companion.toMarker
import com.hanialjti.allchat.data.remote.xmpp.model.ChatMarkerWrapper.Companion.toMessageStatus
import com.hanialjti.allchat.data.remote.xmpp.model.MucBookmark
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jivesoftware.smack.MessageListener
import org.jivesoftware.smack.SmackException
import org.jivesoftware.smack.XMPPException
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smackx.bookmarks.BookmarkManager
import org.jivesoftware.smackx.muc.MultiUserChat
import org.jivesoftware.smackx.muc.MultiUserChatManager
import org.jivesoftware.smackx.pep.PepEventListener
import org.jivesoftware.smackx.pep.PepManager
import org.jivesoftware.smackx.pubsub.AccessModel
import org.jivesoftware.smackx.pubsub.PayloadItem
import org.jivesoftware.smackx.pubsub.PubSubManager
import org.jivesoftware.smackx.pubsub.PublishModel
import org.jxmpp.jid.parts.Resourcepart
import timber.log.Timber
import java.util.*

class MucManager(
    private val connection: XMPPTCPConnection,
    private val clientDataStore: XmppClientDataSource,
    private val dispatcher: CoroutineDispatcher,
    private val mucManager: MultiUserChatManager = MultiUserChatManager.getInstanceFor(connection),
    private val bookmarkManager: BookmarkManager = BookmarkManager.getBookmarkManager(connection),
    private val pepManager: PepManager = PepManager.getInstanceFor(connection),
    private val pubSubManager: PubSubManager = PubSubManager.getInstanceFor(connection),
) {

    private val _subjectStream: MutableSharedFlow<Pair<String, String>> = MutableSharedFlow()
    val subjectStream = _subjectStream.asSharedFlow()

    fun bookmarkedConferencesStream() = callbackFlow {

        if (clientDataStore.shouldSyncChatsStream.first()) {
            val chatRooms = retrieveGroupChats()
            clientDataStore.addChatRooms(*chatRooms.toTypedArray())

            chatRooms.forEach { conference ->
                trySend(
                    NewContactUpdate(
                        conference,
                        isGroupChat = true
                    )
                )
            }
        }

        val listener = PepEventListener<MucBookmark> { _, _, id, carrierMessage ->
            Logger.d { carrierMessage.toString() }
            Logger.d { "New MucBookmark.." }
            launch {
                clientDataStore.addChatRooms(id)
                send(
                    NewContactUpdate(id, true)
                )
            }
        }

        pepManager.addPepEventListener(
            MucBookmark.NAMESPACE,
            MucBookmark::class.java,
            listener
        )
        awaitClose {
            pepManager.removePepEventListener(listener)
        }
    }

    // TODO Support XEP-0048
    private suspend fun retrieveGroupChats() = withContext(dispatcher) {
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


    private suspend fun joinRoom(roomId: String) =
        try {
            Logger.d { "Joining $roomId..." }
            val muc = mucManager.getMultiUserChat(roomId.asJid().asEntityBareJidIfPossible())
            val myNickname = connection.user.localpartOrThrow.toString()
            if (!muc.isJoined) {
                val history = muc.getEnterConfigurationBuilder(Resourcepart.from(myNickname))
                    .requestHistorySince(534776876).build()
                muc.join(history)
            }
            true
        } catch (e: Exception) {
            Logger.e(e)
            false
        }

    fun mucMessagesStream(): Flow<RemoteMessage> = callbackFlow {

        val listeners = mutableMapOf<MultiUserChat, MessageListener>()

        clientDataStore.isAuthenticated
            .collectLatest {
                Logger.d { "isAuthenticated: $it" }
                if (it)
                    clientDataStore
                        .chatRooms
                        .collect { rooms ->
                            rooms
                                .toList()
                                .forEach { roomAddress ->

                                    val muc = mucManager.getMultiUserChat(
                                        roomAddress.asJid().asEntityBareJidIfPossible()
                                    )

                                    val listener = MessageListener { message ->
                                        launch {
                                            if (message.subject != null) {
                                                _subjectStream.emit(roomAddress to message.subject)
                                            } else {
                                                val chatMarker = message.wrapMarker()

                                                send(
                                                    RemoteMessage(
                                                        id = chatMarker?.stanzaId
                                                            ?: message.stanzaId,
                                                        body = if (message.isMessage()) message.body else null,
                                                        chatId = message.fromAsString(),
                                                        sender = message.from?.resourceOrNull
                                                            ?.toString()
                                                            ?.plus("@")
                                                            ?.plus(
                                                                connection.xmppServiceDomain.domain.toString()
                                                            ),
//                                                                ?.asJid()
//                                                                ?.asBareJid()
//                                                                ?.toString(),
                                                        attachment = message.extractAttachment(),
                                                        type = MessageType.GroupChat,
                                                        thread = message.thread,
                                                        messageStatus = chatMarker?.toMessageStatus()
                                                            ?: MessageStatus.Sent,
                                                        markers = chatMarker?.toMarker()?.let {
                                                            mutableMapOf(
                                                                message.from.resourceOrNull.toString()
                                                                    .asJid().asBareJid()
                                                                    .toString() to it
                                                            )
                                                        } ?: mutableMapOf()
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    muc.addMessageListener(listener)
                                    listeners[muc] = listener
                                    joinRoom(roomAddress)
                                }

                        }
            }

        awaitClose {
            listeners.forEach { (muc, listener) ->
                muc.removeMessageListener(listener)
            }
        }

    }

    suspend fun createChatRoom(
        roomName: String,
        myId: String,
        invitees: Set<String>
    ): CallResult<String> = withContext(dispatcher) {

        val roomId = UUID.randomUUID().toString()
        val roomJid = roomId.plus("@").plus(mucManager.mucServiceDomains.first())
        val muc = mucManager.getMultiUserChat(roomJid.asJid().asEntityBareJidIfPossible())

        return@withContext try {
            val pubSubNode = createPubSubNodeForRoom(roomId)

            clientDataStore.addChatRooms(roomJid)

            muc.apply {
                create(Resourcepart.from(myId))
                    .configFormManager
                    .makeMembersOnly()
                    .setPubSubNode(pubSubNode)
                    .submitConfigurationForm()

                changeSubject(roomName)
            }

//            joinRoom(roomJid)
//            muc.leave()

            bookmarkConference(roomJid, roomName)

            invitees.forEach {
                muc.inviteDirectly(it.asJid().asEntityBareJidIfPossible())
            }

            CallResult.Success(roomJid)
        } catch (e: Exception) {
            Timber.e(e)
            muc.destroy("Error while creating the room", null)
            destroyPubSubNodeForRoom(roomId)
            clientDataStore.removeChatRooms(roomJid)
            CallResult.Error("An error occurred while creating chat room", e)
        }
    }

    @kotlin.jvm.Throws(
        XMPPException.XMPPErrorException::class,
        SmackException.NoResponseException::class,
        SmackException.NotConnectedException::class,
        InterruptedException::class
    )
    private fun createPubSubNodeForRoom(roomId: String): String {
        val node = pubSubManager.getOrCreateLeafNode(roomId)

        val nodeConfiguration = node.nodeConfiguration
        val fillableForm = nodeConfiguration.fillableForm

        fillableForm
            .apply {
                accessModel = AccessModel.whitelist
                publishModel = PublishModel.subscribers
                isDeliverPayloads = true
            }
        node.sendConfigurationForm(fillableForm)
        return "xmpp:${node.id}@${pubSubManager.serviceJid}"
    }

    @kotlin.jvm.Throws(
        XMPPException.XMPPErrorException::class,
        SmackException.NoResponseException::class,
        SmackException.NotConnectedException::class,
        InterruptedException::class
    )
    private fun destroyPubSubNodeForRoom(roomId: String) {
        pubSubManager.deleteNode(roomId)
    }

    private suspend fun bookmarkConference(
        chatRoomId: String,
        chatRoomName: String
    ) {
        try {

            if (pepManager.isSupported) {
                val pubSub = pepManager.pepPubSubManager
                val bookmarksNode = pubSub.getOrCreateLeafNode(MucBookmark.NAMESPACE)

                val nodeConfiguration = bookmarksNode.nodeConfiguration
                val fillableForm = nodeConfiguration.fillableForm

                fillableForm.apply {
                    setPersistentItems(true)
                    accessModel = AccessModel.whitelist
                    setAnswer("pubsub#send_last_published_item", "never")
                    maxItems = nodeConfiguration.maxItems
                }

                bookmarksNode.sendConfigurationForm(fillableForm)
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

    suspend fun sendMessageToRoom(chatRoom: String, stanza: Message): Boolean {
        return try {
            joinRoom(chatRoom)
            mucManager
                .getMultiUserChat(chatRoom.asJid().asEntityBareJidIfPossible())
                .sendMessage(stanza.asBuilder())
            true
        } catch (e: Exception) {
            Timber.e(e)
            false
        }
    }

}