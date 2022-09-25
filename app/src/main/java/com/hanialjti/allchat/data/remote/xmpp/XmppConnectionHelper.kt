package com.hanialjti.allchat.data.remote.xmpp

import com.hanialjti.allchat.common.model.ListChange
import com.hanialjti.allchat.common.model.Resource
import com.hanialjti.allchat.data.remote.xmpp.model.AvatarDataExtensionElement
import com.hanialjti.allchat.data.remote.xmpp.model.ChatMarker.Companion.toStatus
import com.hanialjti.allchat.data.local.room.entity.*
import com.hanialjti.allchat.common.utils.currentTimestamp
import com.hanialjti.allchat.data.remote.MessageOperation
import com.hanialjti.allchat.data.remote.MessagePage
import com.hanialjti.allchat.presentation.conversation.ChatState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jivesoftware.smack.SmackException.NoResponseException
import org.jivesoftware.smack.SmackException.NotConnectedException
import org.jivesoftware.smack.StanzaListener
import org.jivesoftware.smack.XMPPException
import org.jivesoftware.smack.chat2.ChatManager
import org.jivesoftware.smack.chat2.IncomingChatMessageListener
import org.jivesoftware.smack.packet.Presence
import org.jivesoftware.smack.roster.Roster
import org.jivesoftware.smack.roster.RosterListener
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smackx.bookmarks.BookmarkManager
import org.jivesoftware.smackx.carbons.CarbonCopyReceivedListener
import org.jivesoftware.smackx.carbons.CarbonManager
import org.jivesoftware.smackx.chat_markers.ChatMarkersListener
import org.jivesoftware.smackx.chat_markers.ChatMarkersManager
import org.jivesoftware.smackx.chat_markers.element.ChatMarkersElements
import org.jivesoftware.smackx.chatstates.ChatStateListener
import org.jivesoftware.smackx.chatstates.ChatStateManager
import org.jivesoftware.smackx.csi.ClientStateIndicationManager
import org.jivesoftware.smackx.hints.element.StoreHint
import org.jivesoftware.smackx.mam.MamManager
import org.jivesoftware.smackx.muc.MultiUserChatManager
import org.jivesoftware.smackx.pubsub.Item
import org.jivesoftware.smackx.pubsub.PayloadItem
import org.jivesoftware.smackx.pubsub.PubSubManager
import org.jivesoftware.smackx.pubsub.listener.ItemEventListener
import org.jivesoftware.smackx.push_notifications.PushNotificationsManager
import org.jivesoftware.smackx.push_notifications.element.PushNotificationsElements
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager
import org.jivesoftware.smackx.receipts.ReceiptReceivedListener
import org.jivesoftware.smackx.time.EntityTimeManager
import org.jivesoftware.smackx.vcardtemp.VCardManager
import org.jivesoftware.smackx.vcardtemp.packet.VCard
import org.jxmpp.jid.Jid
import org.jxmpp.jid.impl.JidCreate
import timber.log.Timber
import java.io.IOException
import java.net.URL
import java.time.Instant
import java.time.ZoneOffset
import java.util.*


class XmppConnectionHelper(
    private val connection: XMPPTCPConnection,
    private val rosterManager: Roster,
    private val carbonManager: CarbonManager,
    private val groupChatManager: GroupChatManager,
    private val receiptManager: DeliveryReceiptManager
) {

    private val mucChatManager = MultiUserChatManager.getInstanceFor(connection)
    private val mamManager = MamManager.getInstanceFor(connection)
    private val vCardManager = VCardManager.getInstanceFor(connection)
    private var pubSubManager = PubSubManager.getInstanceFor(connection, JidCreate.bareFrom("hani@localhost"))
    private val chatManager = ChatManager.getInstanceFor(connection)
    private val chatMarkersManager = ChatMarkersManager.getInstanceFor(connection)
    private val chatStateManager = ChatStateManager.getInstance(connection)
    private val bookmarkManager = BookmarkManager.getBookmarkManager(connection)
    private val timeManager = EntityTimeManager.getInstanceFor(connection)

    suspend fun updateMyChatState(chatState: ChatState) {
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

    private fun observeDeliveryReceipts() = callbackFlow<MessageOperation> {
        val deliveryReceiptListener = ReceiptReceivedListener { from, to, id, receipt ->
            Timber.d("New Delivery receipt from: $from, to: $to, id: $id, receipt: $receipt")
        }

        receiptManager.addReceiptReceivedListener(deliveryReceiptListener)

        awaitClose {
            receiptManager.removeReceiptReceivedListener(deliveryReceiptListener)
        }
    }

    private fun observeAcknowledgmentMessages() = callbackFlow {

        val ackListener = StanzaListener { stanza ->

            if (stanza.isMessageAck()) {
                Timber.d("New Acknowledgment message! $stanza")
                launch {
                    send(
                        MessageOperation.StatusChanged(
                            StatusMessage(
                                remoteId = stanza.stanzaId,
                                timestamp = currentTimestamp,
                                status = Status.Acknowledged,
                                owner = connection.getOwner(),
                                from = connection.getOwner(),
                                conversation = stanza.to.asBareJid().toString()
                            )
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

    private fun observeCarbonCopies() = callbackFlow {
        val carbonListener = CarbonCopyReceivedListener { direction, carbonCopy, wrappingMessage ->
            Timber.d("New carbon copy direction: $direction, carbon: $carbonCopy, wrappingMessage: $wrappingMessage ")
            launch {
                send(
                    MessageOperation.Created(
                        Message(
                            remoteId = carbonCopy.stanzaId,
                            body = carbonCopy.body,
                            timestamp = currentTimestamp,
                            conversation = connection.getConversationIdFromStanza(carbonCopy),
                            from = carbonCopy.getStringFrom(),
                            owner = connection.getOwner(),
                            status = Status.Acknowledged,
                            type = carbonCopy.type.toMessageType()
                        )
                    )
                )
            }
        }

        carbonManager.addCarbonCopyReceivedListener(carbonListener)

        awaitClose {
            carbonManager.removeCarbonCopyReceivedListener(carbonListener)
        }
    }

    fun observeChatStates() = callbackFlow {
        val chatStateListener = ChatStateListener { _, state, message ->
            val conversation = message.from.asBareJid().toString()
            val from = message.from.asBareJid().localpartOrNull?.toString()

            from?.let {
                launch { send(state.toConversationState(conversation, it)) }
            }

        }
        chatStateManager.addChatStateListener(chatStateListener)

        awaitClose { chatStateManager.removeChatStateListener(chatStateListener) }
    }

    private suspend fun MamManager.MamQueryArgs.getMessagePage(): MessagePage {
        return try {
            val query = mamManager.queryArchive(this)
            val page = query.page
            val messages = page.messages
            val forwardedMessages = page.forwarded

            MessagePage(
                messageList = messages.zip(forwardedMessages) { message, forwarded ->
                    if (message.hasExtension(ChatMarkersElements.DisplayedExtension.QNAME)) {
                        val displayedMarker = message.getChatMarker()
                        MessageOperation.StatusChanged(
                            StatusMessage(
                                remoteId = displayedMarker?.stanzaId,
                                timestamp = forwarded.timestamp(),
                                status = Status.Seen,
                                owner = connection.getOwner(),
                                from = message.getStringFrom(),
                                conversation = connection.getConversationIdFromStanza(message)
                            )
                        )
                    } else {
                        MessageOperation.Created(
                            Message(
                                remoteId = message.stanzaId,
                                body = message.body,
                                timestamp = forwarded.timestamp(),
                                conversation = connection.getConversationIdFromStanza(message),
                                from = message.getStringFrom(),
                                owner = connection.getOwner(),
                                status = Status.Acknowledged,
                                type = message.type.toMessageType()
                            )
                        )
                    }
                },
                isComplete = query.isComplete,
                error = null
            )
        } catch (e: Exception) {
            MessagePage(isComplete = false, error = e)
        }
    }

    private fun getDateWithOffset(localEpochTime: Long, offset: String): Date {
        val epochMilliWithOffset =
            Instant.ofEpochMilli(localEpochTime).atOffset(ZoneOffset.of(offset)).toInstant()
                .toEpochMilli()
        return Date(epochMilliWithOffset)
    }

    suspend fun syncMessages(
        afterMessage: Message?,
        pageSize: Int
    ): MessagePage {

        val serverOffset = timeManager.getTime(connection.xmppServiceDomain).tzo
        val date = afterMessage?.timestamp?.let { getDateWithOffset(it, serverOffset) }

        val pageArgs = MamManager.MamQueryArgs
            .builder()
            .setResultPageSize(pageSize)
            .apply {
                if (afterMessage == null) {
                    queryLastPage()
                } else {
                    limitResultsSince(date)
                }
            }
            .build()

        return pageArgs.getMessagePage()
    }

    suspend fun getPreviousPage(
        beforeMessage: Message?,
        conversationId: String? = null,
        pageSize: Int
    ): MessagePage {
        return try {

            val date = beforeMessage?.timestamp?.let { Date(it) }
            val pageArgs = MamManager.MamQueryArgs
                .builder()
                .queryLastPage()
                .setResultPageSize(pageSize)
                .apply {
                    if (conversationId != null) {
                        limitResultsToJid(conversationId.asJid())
                    }
                    if (beforeMessage?.timestamp != null) {
                        limitResultsBefore(date)
                    }
                }
                .build()

            val query = mamManager.queryArchive(pageArgs)
            val page = query.page
            val messages = page.messages
            val forwardedMessages = page.forwarded

            MessagePage(
                messageList = messages.zip(forwardedMessages) { message, forwarded ->
                    if (message.hasExtension(ChatMarkersElements.DisplayedExtension.QNAME)) {
                        val displayedMarker = message.getChatMarker()
                        MessageOperation.StatusChanged(
                            StatusMessage(
                                remoteId = displayedMarker?.stanzaId,
                                timestamp = forwarded.timestamp(),
                                status = Status.Seen,
                                owner = connection.getOwner(),
                                from = message.getStringFrom(),
                                conversation = connection.getConversationIdFromStanza(message)
                            )
                        )
                    } else {
                        MessageOperation.Created(
                            Message(
                                remoteId = message.stanzaId,
                                body = message.body,
                                timestamp = forwarded.timestamp(),
                                conversation = connection.getConversationIdFromStanza(message),
                                from = message.getStringFrom(),
                                owner = connection.getOwner(),
                                status = Status.Acknowledged,
                                type = message.type.toMessageType()
                            )
                        )
                    }
                },
                isComplete = query.isComplete,
                error = null
            )
        } catch (e: Exception) {
            MessagePage(isComplete = false, error = e)
        }
    }

    private fun observeOneOnOneMessages() = callbackFlow {
        val chatListener = IncomingChatMessageListener { from, message, chat ->
            Timber.d("Received a 1:1 message from: $from, message: $message, chat: $chat")
            launch {
                message?.stanzaId?.let { stanzaId ->
                    send(
                        MessageOperation.Created(
                            Message(
                                remoteId = stanzaId,
                                body = message.body,
                                conversation = connection.getConversationIdFromStanza(message),
                                from = message.from.asBareJid().localpartOrNull?.toString(),
                                status = Status.Received,
                                type = message.type.toMessageType(),
                                owner = connection.getOwner()
                            )
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

            val chatMarker = message.getChatMarker()

            chatMarker?.let {
                launch {
                    send(
                        MessageOperation.StatusChanged(
                            StatusMessage(
                                remoteId = chatMarker.stanzaId,
                                timestamp = currentTimestamp,
                                status = chatMarker.toStatus(),
                                owner = connection.getOwner(),
                                from = message.getStringFrom(),
                                conversation = connection.getConversationIdFromStanza(message)
                            )
                        )
                    )
                }
            }
        }

        chatMarkersManager.addIncomingChatMarkerMessageListener(chatMarkersListener)

        awaitClose {
            chatMarkersManager.removeIncomingChatMarkerMessageListener(chatMarkersListener)
        }
    }

    fun listenForMessageChanges() = merge(
        observeOneOnOneMessages(),
        observeChatMarkers(),
        observeAcknowledgmentMessages(),
        observeCarbonCopies(),
        observeNewMessages()
    )

    private fun observeNewMessages() = callbackFlow {
        val listener = StanzaListener {
            Timber.d("Received a message $it")
            val message = it as? org.jivesoftware.smack.packet.Message
            launch {
                message?.stanzaId?.let { stanzaId ->
                    send(
                        MessageOperation.Created(
                            Message(
                                remoteId = stanzaId,
                                body = message.body,
                                conversation = connection.getConversationIdFromStanza(message),
                                from = message.from.asBareJid().localpartOrNull?.toString(),
                                status = Status.Received,
                                type = message.type.toMessageType(),
                                owner = connection.getOwner()
                            )
                        )
                    )
                }
            }
        }

        connection.addStanzaListener(listener) {
            it is org.jivesoftware.smack.packet.Message &&
                    it.type == org.jivesoftware.smack.packet.Message.Type.groupchat &&
                    it.type != org.jivesoftware.smack.packet.Message.Type.error
        }

        awaitClose {
            connection.removeStanzaListener(listener)
        }
    }

    suspend fun updateMyInfo(
        user: User,
        onFailure: (Throwable) -> Unit
    ) = withContext(Dispatchers.IO) {

        try {

            val vCard = VCard().also { vCard ->
                user.name?.let { name -> vCard.firstName = name }
            }

            user.image?.let { imageUrl ->
                URL(imageUrl)
                    .openConnection()
                    .getInputStream()
                    .use { inputStream ->
                        vCard.avatar = inputStream.readBytes()
                    }
            }

            vCardManager.saveVCard(vCard)

        } catch (e: IOException) {
            onFailure(e)
        } catch (e: XMPPException.XMPPErrorException) {
            onFailure(e)
        } catch (e: NoResponseException) {
            onFailure(e)
        } catch (e: NotConnectedException) {
            onFailure(e)
        } catch (e: InterruptedException) {
            onFailure(e)
        }
    }

    suspend fun getUpdatedUserInfo(user: User) = withContext(Dispatchers.IO) {
        return@withContext vCardManager.loadVCard(JidCreate.entityBareFrom(user.id)).let { vCard ->
            user.copy(
                name = vCard.firstName,
                image = vCard.avatar.toString(),
            )
        }
    }

    suspend fun getParticipantsInfo(conversation: String) = withContext(Dispatchers.IO) {
        return@withContext mucChatManager
            .getMultiUserChat(JidCreate.entityBareFrom(conversation))
            .participants
            .map {
                getUpdatedUserInfo(
                    User(it.jid.asBareJid().toString())
                )
            }
    }

    suspend fun listenForUserImageUpdates(user: User) = callbackFlow {

        pubSubManager = PubSubManager.getInstanceFor(connection, JidCreate.bareFrom(user.id))

        val itemEventListener = ItemEventListener<Item> {

            val items = it.items
            items.forEach { item ->
                if (item is PayloadItem<*>) {
                    if (item.payload is AvatarDataExtensionElement) {
                        val payloadItem = item as PayloadItem<AvatarDataExtensionElement>
                        launch { send(user.copy(image = payloadItem.payload.data)) }
                    }
                }
            }
        }

        val node = pubSubManager.getLeafNode("urn:xmpp:avatar:data")

        node.addItemEventListener(itemEventListener)

        awaitClose {
            node.removeItemEventListener(itemEventListener)
        }

    }


    fun listenForRosterChanges() = callbackFlow {
        val rosterListener = object : RosterListener {
            override fun entriesAdded(addresses: MutableCollection<Jid>?) {
                addresses?.forEach { jid ->
                    connection.getOwner()?.let { owner ->
                        launch {
                            send(
                                ListChange.ItemAdded(
                                    Conversation(
                                        id = jid.asBareJid().toString(),
                                        isGroupChat = jid.isGroupConversation(),
                                        from = owner
                                    )
                                )
                            )
                        }
                    }
                }
            }

            override fun entriesUpdated(addresses: MutableCollection<Jid>?) {
                Timber.d("Roster entries added")
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
                        send(
                            ListChange.ItemUpdated(
                                User(
                                    id = jid.asBareJid().toString(),
                                    isOnline = isOnline
                                )
                            )
                        )
                    }
                }
            }
        }

        rosterManager.addRosterListener(rosterListener)

        awaitClose {
            rosterManager.removeRosterListener(rosterListener)
        }
    }

    suspend fun markMessageAsDisplayed(message: Message) = withContext(Dispatchers.IO) {

        fun getStatusMessage(status: Status) =
            StatusMessage(
                remoteId = message.remoteId,
                timestamp = currentTimestamp,
                status = status,
                owner = connection.getOwner(),
                from = message.from,
                conversation = message.conversation
            )


        val stanza = connection.stanzaFactory
            .buildMessageStanza()
            .to(message.conversation)
            .addExtension(ChatMarkersElements.DisplayedExtension(message.remoteId))
            .addExtension(StoreHint.INSTANCE)
            .build()

        return@withContext try {
            connection.sendStanza(stanza)
            MessageOperation.StatusChanged(
                getStatusMessage(Status.Seen)
            )
        } catch (e: NotConnectedException) {
            MessageOperation.Error(getStatusMessage(Status.Error), e)
        } catch (e: InterruptedException) {
            MessageOperation.Error(getStatusMessage(Status.Error), e)
        }
    }

    private suspend fun <T> tryExecute(func: suspend () -> T): Resource<T> {
        return try {
            Resource.Success(func())
        } catch (e: IOException) {
            Resource.Error(cause = e)
        } catch (e: XMPPException.XMPPErrorException) {
            Resource.Error(cause = e)
        } catch (e: NoResponseException) {
            Resource.Error(cause = e)
        } catch (e: NotConnectedException) {
            Resource.Error(cause = e)
        } catch (e: InterruptedException) {
            Resource.Error(cause = e)
        }
    }

    fun createMessageStanzaFromMessage(message: Message) =
        message.toMessageStanza(connection)

    suspend fun sendMessage(message: Message): Resource<Message> =
        withContext(Dispatchers.IO) {

            val stanzaMessage = connection.stanzaFactory
                .buildMessageStanza()
                .to(message.conversation)
                .ofType(message.type?.toXmppMessageType())
                .setBody(message.body)
                .addExtension(ChatMarkersElements.MarkableExtension.INSTANCE)
                .build()

            return@withContext tryExecute {
                if (message.type == Type.Chat) {
                    chatManager.chatWith(message.conversation?.asJid()?.asEntityBareJidIfPossible())
                        .send(stanzaMessage)
                }
                //TODO send to muc if group chat
//                connection.sendStanza(stanzaMessage)
                return@tryExecute message.copy(
                    remoteId = stanzaMessage.stanzaId,
                    status = Status.Sent
                )
            }

        }

    suspend fun retrieveContacts(owner: String) = withContext(Dispatchers.IO) {
        val conferences = try {
            bookmarkManager
                .bookmarkedConferences
                .map { bookmarkedConference ->
                    val vCard = vCardManager.loadVCard(bookmarkedConference.jid)
                    ConversationAndUser(
                        conversation = Conversation(
                            id = bookmarkedConference.jid.toString(),
                            isGroupChat = true,
                            name = bookmarkedConference.name,
                            from = owner,
                            imageUrl = vCard.avatar.toString()
                        ),
                        user = null
                    )
                }
        } catch (e: Exception) {
            Timber.e(e)
            listOf()
        }

        val users = rosterManager.entries
            .map { rosterEntry ->
                val vCard = try {
                    vCardManager.loadVCard(rosterEntry.jid.asEntityBareJidIfPossible())
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
                ConversationAndUser(
                    conversation = Conversation(
                        id = rosterEntry.jid.toString(),
                        isGroupChat = false,
                        to = rosterEntry.jid.toString(),
                        from = owner
                    ),
                    user = User(
                        id = rosterEntry.jid.toString(),
                        name = vCard?.nickName ?: rosterEntry.name,
                        image = vCard?.avatar?.toString()
                    )
                )
            }

        return@withContext conferences.plus(users)
    }

    // TODO: Start the conversation via a worker
    suspend fun startConversation(conversationId: String, name: String, myId: String) {

        if (conversationId.asJid().isGroupConversation()) {
            groupChatManager.addGroupChatToContacts(conversationId, name, myId)
        } else {
            addOneOnOneConversationToRoster(
                conversationId,
                name,
                onFailure = { Timber.d("Failed to add user to contacts", it) }
            )
        }
    }

    private suspend fun <T> run(
        run: suspend () -> T,
        onFailure: (Throwable) -> Unit
    ) {
        try {
            run()
        } catch (e: IOException) {
            onFailure(e)
        } catch (e: XMPPException.XMPPErrorException) {
            onFailure(e)
        } catch (e: NoResponseException) {
            onFailure(e)
        } catch (e: NotConnectedException) {
            onFailure(e)
        } catch (e: InterruptedException) {
            onFailure(e)
        }
    }

    private suspend fun addOneOnOneConversationToRoster(
        id: String,
        name: String,
        onFailure: (Throwable) -> Unit
    ) = run(
        {
            rosterManager.createItemAndRequestSubscription(
                id.asJid(),
                name,
                null
            )
        }
    ) { onFailure(it) }

}