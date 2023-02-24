package com.hanialjti.allchat.data.remote.xmpp

import com.hanialjti.allchat.data.local.room.entity.MessageEntity
import com.hanialjti.allchat.data.model.Marker
import com.hanialjti.allchat.data.model.MessageStatus
import com.hanialjti.allchat.data.model.MessageType
import com.hanialjti.allchat.data.remote.MessageRemoteDataSource
import com.hanialjti.allchat.data.remote.model.*
import com.hanialjti.allchat.data.remote.xmpp.model.ChatMarkerWrapper.Companion.toMarker
import com.hanialjti.allchat.data.remote.xmpp.model.ChatMarkerWrapper.Companion.toMessageStatus
import com.hanialjti.allchat.data.remote.xmpp.model.OutOfBandData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jivesoftware.smack.SmackException.NotConnectedException
import org.jivesoftware.smack.StanzaListener
import org.jivesoftware.smack.chat2.ChatManager
import org.jivesoftware.smack.chat2.IncomingChatMessageListener
import org.jivesoftware.smack.filter.AndFilter
import org.jivesoftware.smack.filter.MessageTypeFilter
import org.jivesoftware.smack.filter.NotFilter
import org.jivesoftware.smack.filter.StanzaTypeFilter
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.packet.MessageBuilder
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smackx.carbons.CarbonCopyReceivedListener
import org.jivesoftware.smackx.carbons.CarbonManager
import org.jivesoftware.smackx.carbons.packet.CarbonExtension
import org.jivesoftware.smackx.chat_markers.ChatMarkersListener
import org.jivesoftware.smackx.chat_markers.ChatMarkersManager
import org.jivesoftware.smackx.chat_markers.element.ChatMarkersElements
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager
import org.jivesoftware.smackx.geoloc.packet.GeoLocation
import org.jivesoftware.smackx.hints.element.NoStoreHint
import org.jivesoftware.smackx.hints.element.StoreHint
import org.jivesoftware.smackx.mam.MamManager
import org.jivesoftware.smackx.muc.MultiUserChatManager
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager
import org.jivesoftware.smackx.receipts.ReceiptReceivedListener
import org.jxmpp.jid.impl.JidCreate
import org.jxmpp.jid.parts.Resourcepart
import timber.log.Timber
import java.time.Instant
import java.time.ZoneOffset


class XmppRemoteDataSource(
    private val connection: XMPPTCPConnection,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : MessageRemoteDataSource {

    private val mamManager = MamManager.getInstanceFor(connection)
    private val chatManager = ChatManager.getInstanceFor(connection)
    private val chatMarkersManager = ChatMarkersManager.getInstanceFor(connection)
    private val carbonManager = CarbonManager.getInstanceFor(connection)
    private val receiptManager = DeliveryReceiptManager.getInstanceFor(connection)
    private val mucManager = MultiUserChatManager.getInstanceFor(connection)
    private val serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(connection)

    private val messagesWaitingForAcknowledgment: MutableList<String> = mutableListOf()

    private fun observeDeliveryReceipts() = callbackFlow {
        val deliveryReceiptListener = ReceiptReceivedListener { from, to, id, receipt ->
            Timber.d("New Delivery receipt from: $from, to: $to, id: $id, receipt: $receipt")
            launch {
                send(
                    RemoteMessage(
                        id = id,
                        messageStatus = MessageStatus.Delivered,
                    )
                )
            }
        }

        receiptManager.addReceiptReceivedListener(deliveryReceiptListener)

        awaitClose {
            receiptManager.removeReceiptReceivedListener(deliveryReceiptListener)
        }
    }

    fun observeAcknowledgmentMessages() = callbackFlow {

        val ackListener = StanzaListener { stanza ->
            if (stanza.isMessageAck() && messagesWaitingForAcknowledgment.contains(stanza.stanzaId)) {
                Timber.d("New Acknowledgment message! $stanza")
                launch {
                    send(
                        RemoteMessage(
                            id = stanza.stanzaId,
                            messageStatus = MessageStatus.Sent,
                        )
                    )
                }
            }
        }

        connection.addStanzaAcknowledgedListener(ackListener)

        awaitClose {
            connection.removeStanzaAcknowledgedListener(ackListener)
        }

    }


    private fun observeCarbonCopiedMessages() = callbackFlow {
        val carbonListener = CarbonCopyReceivedListener { direction, carbonCopy, wrappingMessage ->
            Timber.d("New carbon copy direction: $direction, carbon: $carbonCopy, wrappingMessage: $wrappingMessage ")

            if (carbonCopy.isMessage() && carbonCopy.stanzaId != null) {
                if (carbonCopy.isMucInvitation()) {
                    if (direction == CarbonExtension.Direction.received) {
                        val mucInvitation = carbonCopy.extractDirectMucInvitation()
                        mucInvitation?.let {
                            launch {
                                send(it)
                            }
                        }
                    }
                } else if (!carbonCopy.hasExtension(NoStoreHint.NAMESPACE)) {

                    val chatMarker = carbonCopy.wrapMarker()
                    val sender = carbonCopy.fromAsString()

                    launch {
                        send(
                            RemoteMessage(
                                id = chatMarker?.stanzaId ?: carbonCopy.stanzaId,
                                body = if (carbonCopy.isMessage()) carbonCopy.body else null,
                                chatId = if (direction == CarbonExtension.Direction.sent) carbonCopy.toAsString() else carbonCopy.fromAsString(),
                                type = MessageType.Chat, // since group chat messages should not be carbon copied
                                sender = sender,
                                thread = carbonCopy.thread,
                                sentTo = carbonCopy.to.resourceOrNull.toString(),
                                attachment = carbonCopy.extractAttachment(),
                                messageStatus = if (direction == CarbonExtension.Direction.sent) MessageStatus.Sent else MessageStatus.Delivered,
                                markers = if (chatMarker != null && sender != null) mapOf(sender to chatMarker.toMarker()) else mapOf()
                            )
                        )
                    }
                }
            }
        }

        carbonManager.addCarbonCopyReceivedListener(carbonListener)

        awaitClose {
            carbonManager.removeCarbonCopyReceivedListener(carbonListener)
        }
    }

    private fun Message.extractAttachment(): RemoteAttachment? {
        val mediaAttachment = getExtension(OutOfBandData.NAMESPACE) as? OutOfBandData
        val locationAttachment = getExtension(GeoLocation.NAMESPACE) as? GeoLocation

        return when {
            mediaAttachment != null -> Media(
                url = mediaAttachment.url,
                desc = mediaAttachment.desc
            )
            locationAttachment != null -> Location(
                lat = locationAttachment.lat,
                lng = locationAttachment.lon
            )
            else -> null
        }
    }

    private suspend fun MamManager.MamQueryArgs.getMessagePage(): MessagePage =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val query = mamManager.queryArchive(this@getMessagePage)
                val page = query.page
                val mamResultExtensions = page.mamResultExtensions

                MessagePage(
                    messageList = mamResultExtensions
                        .filter { it.forwarded.forwardedStanza.stanzaId != null }
                        .map { mamResultExtension ->

                            val forwarded = mamResultExtension.forwarded
                            val message = forwarded.forwardedStanza
                            val sender = message.fromAsString()
                            val chatMarker = message.wrapMarker()

                            RemoteMessage(
                                id = chatMarker?.stanzaId ?: message.stanzaId,
                                body = if (message.isMessage()) message.body else null,
                                chatId = connection.getConversationIdFromStanza(message),
                                sender = sender,
                                type = message.type.toMessageType(),
                                thread = message.thread,
                                messageStatus = MessageStatus.Sent,
                                timestamp = Instant.ofEpochMilli(forwarded.timestamp()).atOffset(
                                    ZoneOffset.UTC
                                ),
                                attachment = message.extractAttachment(),
                                messageArchiveId = mamResultExtension.id,
                                markers = if (chatMarker != null && sender != null) mapOf(sender to chatMarker.toMarker()) else mapOf()
                            )
//                            chatMarker?.let { markerWrapper ->
//                                savableMessage.sender?.let {
//                                    savableMessage.copy(
//                                        id = markerWrapper.stanzaId,
//                                        markers = mutableMapOf(savableMessage.sender to chatMarker.toMarker()),
//                                    )
//                                }
//                            } ?: savableMessage
                        },
                    isComplete = query.isComplete,
                    error = null
                )
            } catch (e: Exception) {
                MessagePage(isComplete = true, error = e)
            }
        }

    override suspend fun syncMessages(
        lastMessage: RemoteMessage?,
        pageSize: Int
    ): MessagePage {

        val pageArgs = MamManager.MamQueryArgs
            .builder()
            .setResultPageSize(pageSize)
            .apply {
                if (lastMessage == null) {
                    queryLastPage()
                } else {
                    afterUid(lastMessage.messageArchiveId)
                }
            }
            .build()

        val messagePage = pageArgs.getMessagePage()
        return messagePage.copy(isComplete = messagePage.messageList.count() <= pageSize)
    }

    override suspend fun getPreviousPage(
        chatId: String,
        oldestMessage: RemoteMessage?,
        pageSize: Int
    ): MessagePage {
        return try {

            val pageArgs = MamManager.MamQueryArgs
                .builder()
                .queryLastPage()
                .setResultPageSize(pageSize)
                .apply {
                    limitResultsToJid(chatId.asJid())
                    beforeUid(oldestMessage?.messageArchiveId)
                }
                .build()

            val messagePage = pageArgs.getMessagePage()
            return messagePage.copy(isComplete = messagePage.messageList.count() <= pageSize)

        } catch (e: Exception) {
            MessagePage(isComplete = true, error = e)
        }
    }

    override suspend fun getNextPage(
        chatId: String,
        newestMessage: RemoteMessage?,
        pageSize: Int
    ): MessagePage {
        return try {
            val pageArgs = MamManager.MamQueryArgs
                .builder()
                .setResultPageSize(pageSize)
                .apply {
                    limitResultsToJid(chatId.asJid())
                    afterUid(newestMessage?.messageArchiveId)
                }
                .build()

            val messagePage = pageArgs.getMessagePage()
            return messagePage.copy(isComplete = messagePage.messageList.count() <= pageSize)

        } catch (e: Exception) {
            MessagePage(isComplete = true, error = e)
        }
    }

    private fun observeOneOnOneMessages() = callbackFlow {
        val chatListener = IncomingChatMessageListener { from, message, chat ->
            Timber.d("Received a 1:1 message from: $from, message: $message, chat: $chat")
            launch {
                message?.stanzaId?.let { _ ->
                    send(
                        RemoteMessage(
                            id = message.stanzaId,
                            chatId = message.fromAsString(),
                            body = if (message.isMessage()) message.body else null,
                            sender = from?.asBareJid()?.toString(),
                            type = MessageType.Chat,
                            thread = message.thread,
                            attachment = message.extractAttachment(),
                            messageStatus = MessageStatus.Delivered
                        )
                    )
                }
            }
        }

        chatManager.addIncomingListener(chatListener)
        awaitClose {
            chatManager.removeIncomingListener(chatListener)
        }
    }

    private fun observeChatMarkers() = callbackFlow {
        val chatMarkersListener = ChatMarkersListener { _, message, _ ->

            val chatMarker = message.wrapMarker()

            chatMarker?.let { _ ->
                message.fromAsString()?.let {
                    launch {
                        send(
                            RemoteMessage(
                                id = chatMarker.stanzaId,
                                markers = mutableMapOf(it to chatMarker.toMarker()),
                                messageStatus = chatMarker.toMessageStatus(),
                                chatId = if (message.type.toMessageType() == MessageType.GroupChat) message.toAsString() else message.fromAsString()
                            )
                        )
                    }
                }
            }
        }

        chatMarkersManager.addIncomingChatMarkerMessageListener(chatMarkersListener)

        awaitClose {
            chatMarkersManager.removeIncomingChatMarkerMessageListener(chatMarkersListener)
        }
    }

    override fun messageChangesStream(): Flow<RemoteMessageItem> = merge(
        observeOneOnOneMessages(),
        observeChatMarkers(),
        observeAcknowledgmentMessages(),
        observeCarbonCopiedMessages(),
        observeNewGroupMessages(),
        observeDeliveryReceipts()
    )

    private fun observeNewGroupMessages() = callbackFlow {

        val listener = StanzaListener { stanza ->
            Timber.d("Received a group message $stanza")

            if (stanza.stanzaId == null) {
                Timber.e("Received message has no stanzaId and therefore can not be saved to local storage")
                return@StanzaListener
            }
            if (stanza is Message) {
                val chatMarker = stanza.wrapMarker()
                launch {
                    send(
                        RemoteMessage(
                            id = chatMarker?.stanzaId ?: stanza.stanzaId,
                            body = if (stanza.isMessage()) stanza.body else null,
                            chatId = stanza.fromAsString(),
                            sender = stanza.from.resourceOrNull.toString().asJid().asBareJid()
                                .toString(),
                            attachment = stanza.extractAttachment(),
                            type = MessageType.GroupChat,
                            thread = stanza.thread,
                            messageStatus = chatMarker?.toMessageStatus() ?: MessageStatus.Sent,
                            markers = chatMarker?.toMarker()?.let {
                                mutableMapOf(
                                    stanza.from.resourceOrNull.toString().asJid().asBareJid()
                                        .toString() to it
                                )
                            } ?: mutableMapOf()
                        )
                    )
                }
            } else {
                Timber.e("Received stanza is not a message: $stanza")
            }

        }

        connection.addStanzaListener(
            listener,
            AndFilter(
                StanzaTypeFilter.MESSAGE,
                MessageTypeFilter.GROUPCHAT,
                NotFilter(MessageTypeFilter.ERROR)
            )
        )

        awaitClose {
            connection.removeStanzaListener(listener)
        }
    }

    override suspend fun updateMarkerForMessage(message: RemoteMessage, marker: Marker) =
        withContext(Dispatchers.IO) {

            if (message.chatId == null) return@withContext CallResult.Error(
                "The message must have a chatId to be sent"
            )

            val stanza = connection.stanzaFactory
                .buildMessageStanza()
                .to(message.chatId)
                .apply {
                    if (marker == Marker.Delivered) {
                        addExtension(ChatMarkersElements.ReceivedExtension(message.id))
                    } else {
                        addExtension(ChatMarkersElements.DisplayedExtension(message.id))
                    }
                }
                .ofType(message.type?.toXmppMessageType())
                .addExtension(StoreHint.INSTANCE)
                .build()

            return@withContext try {
                sendMessage(
                    stanza,
                    to = message.chatId,
                    isGroupChat = message.type == MessageType.GroupChat
                )
                CallResult.Success(message.id)
            } catch (e: NotConnectedException) {
                CallResult.Error("User is not connected!", e)
            } catch (e: InterruptedException) {
                CallResult.Error("An error occurred while sending message", e)
            }
        }

    /**
     * @return the stanza id associated with this message
     */
    override suspend fun sendMessage(
        message: MessageEntity,
        thread: String?,
        isMarkable: Boolean
    ): CallResult<String> = withContext(dispatcher) {

        val stanzaMessage = MessageBuilder.buildMessage(message.id)
            .to(message.contactId)
            .ofType(message.type?.toXmppMessageType())
            .setBody(message.body)
            .apply {
                if (thread != null) setThread(thread)
                if (isMarkable && serviceDiscoveryManager.supportsFeature(
                        JidCreate.bareFrom(
                            message.contactId
                        ), ChatMarkersElements.NAMESPACE
                    )
                ) {
                    addExtension(ChatMarkersElements.MarkableExtension.INSTANCE)
                }
                when (val attachment = message.attachment) {
                    is com.hanialjti.allchat.data.model.Media -> {
                        if (attachment.url != null) {
                            addExtension(
                                OutOfBandData(
                                    url = attachment.url,
                                    desc = attachment.fileName
                                )
                            )
                        }
                    }
                    is com.hanialjti.allchat.data.model.Location -> {
                        addExtension(
                            GeoLocation.builder()
                                .setLat(attachment.lat)
                                .setLon(attachment.lng)
                                .build()
                        )
                    }
                    else -> {}
                }
                if (message.body.isNullOrEmpty() && message.attachment != null) {
                    addExtension(StoreHint.INSTANCE)
                }
            }
            .build()

        messagesWaitingForAcknowledgment.add(stanzaMessage.stanzaId)

        if (message.contactId == null)
            return@withContext CallResult.Error("The message must have a chatId to be sent")

        sendMessage(
            stanzaMessage = stanzaMessage,
            to = message.contactId,
            isGroupChat = message.type == MessageType.GroupChat
        )
    }

    private suspend fun sendMessage(stanzaMessage: Message, to: String, isGroupChat: Boolean) =
        try {
            if (!isGroupChat) {
                chatManager.chatWith(to.asJid().asEntityBareJidIfPossible())
                    .send(stanzaMessage)
            } else {
                val sent = sendMessageToRoom(
                    to,
                    stanzaMessage
                )

                if (!sent) CallResult.Error("Error while sending group chat message")
            }
            CallResult.Success(stanzaMessage.stanzaId)
        } catch (e: Exception) {
            Timber.e(e)
            CallResult.Error("An error occurred while sending message!", e)
        }


    private suspend fun sendMessageToRoom(
        chatRoom: String,
        stanza: Message
    ): Boolean {
        return try {
            if (!mucManager.getMultiUserChat(
                    chatRoom.asJid().asEntityBareJidIfPossible()
                ).isJoined
            ) {
                connection.user.toString().let { joinRoom(chatRoom, it) }
            }
            mucManager.getMultiUserChat(chatRoom.asJid().asEntityBareJidIfPossible())
                .sendMessage(stanza.asBuilder())
            true
        } catch (e: Exception) {
            Timber.e(e)
            false
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


}