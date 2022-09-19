package com.hanialjti.allchat.xmpp

import com.hanialjti.allchat.models.*
import com.hanialjti.allchat.models.entity.*
import com.hanialjti.allchat.utils.currentTimestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jivesoftware.smack.SmackException.NoResponseException
import org.jivesoftware.smack.SmackException.NotConnectedException
import org.jivesoftware.smack.StanzaListener
import org.jivesoftware.smack.XMPPException
import org.jivesoftware.smack.chat2.ChatManager
import org.jivesoftware.smack.packet.Presence
import org.jivesoftware.smack.packet.Stanza
import org.jivesoftware.smack.packet.StanzaBuilder
import org.jivesoftware.smack.packet.StanzaFactory
import org.jivesoftware.smack.packet.id.StanzaIdSource
import org.jivesoftware.smack.roster.Roster
import org.jivesoftware.smack.roster.RosterListener
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smackx.bookmarks.BookmarkManager
import org.jivesoftware.smackx.chat_markers.ChatMarkersListener
import org.jivesoftware.smackx.chat_markers.ChatMarkersManager
import org.jivesoftware.smackx.chat_markers.ChatMarkersState
import org.jivesoftware.smackx.chat_markers.element.ChatMarkersElements
import org.jivesoftware.smackx.chatstates.ChatStateListener
import org.jivesoftware.smackx.chatstates.ChatStateManager
import org.jivesoftware.smackx.hints.element.StoreHint
import org.jivesoftware.smackx.httpfileupload.HttpFileUploadManager
import org.jivesoftware.smackx.mam.MamManager
import org.jivesoftware.smackx.muc.MultiUserChatManager
import org.jivesoftware.smackx.pubsub.Item
import org.jivesoftware.smackx.pubsub.PayloadItem
import org.jivesoftware.smackx.pubsub.PubSubManager
import org.jivesoftware.smackx.pubsub.listener.ItemEventListener
import org.jivesoftware.smackx.vcardtemp.VCardManager
import org.jivesoftware.smackx.vcardtemp.packet.VCard
import org.jxmpp.jid.Jid
import org.jxmpp.jid.impl.JidCreate
import org.jxmpp.jid.parts.Resourcepart
import timber.log.Timber
import java.io.IOException
import java.net.URL
import java.util.*


class XmppConnectionHelper(
    private val connection: XMPPTCPConnection
) {

    private val waitingForAcknowledgment = MutableStateFlow<Message?>(null)

    private val mucChatManager get() = MultiUserChatManager.getInstanceFor(connection)
    private val mamManager get() = MamManager.getInstanceFor(connection)
    private val uploadManager get() = HttpFileUploadManager.getInstanceFor(connection)
    private val rosterManager get() = Roster.getInstanceFor(connection)
    private val vCardManager get() = VCardManager.getInstanceFor(connection)
    private var pubSubManager = PubSubManager.getInstanceFor(
        connection,
        JidCreate.bareFrom("hani@localhost")
    )
    private val chatManager = ChatManager.getInstanceFor(connection)
    private val chatStateManager = ChatStateManager.getInstance(connection)
    private val bookmarkManager get() = BookmarkManager.getBookmarkManager(connection)
    private val chatMarkersManager get() = ChatMarkersManager.getInstanceFor(connection)

    init {
        connection.setUseStreamManagement(true)
        connection.setUseStreamManagementResumption(true)

        rosterManager.subscriptionMode = Roster.SubscriptionMode.accept_all
    }

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

    private fun observeAcknowledgmentMessages() = callbackFlow {

        awaitClose {
            connection.removeAllStanzaIdAcknowledgedListeners()
        }


        waitingForAcknowledgment.collect { message ->
            if (message != null) {
                val listener = StanzaListener { stanza ->
                    launch {
                        send(
                            MessageOperation.StatusChanged(
                                StatusMessage(
                                    id = message.id,
                                    timestamp = currentTimestamp,
                                    status = Status.Sent,
                                    owner = connection.getOwner(),
                                    from = message.from,
                                    conversation = message.conversation
                                )
                            )
                        )
                    }
                }

                connection.addStanzaIdAcknowledgedListener(message.id, listener)
            }
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

    suspend fun getPreviousPage(
        beforeMessage: Message?,
        conversationId: String? = null,
        pageSize: Int
    ): MessagePage {
        return try {

            val pageArgs = if (beforeMessage == null) MamManager.MamQueryArgs
                .builder()
                .queryLastPage()
                .setResultPageSize(pageSize).apply {
                    if (conversationId != null) {
                        limitResultsToJid(conversationId.asJid())
                    }
                }
                .build()
            else
                MamManager.MamQueryArgs.builder()
                    .queryLastPage()
                    .setResultPageSize(pageSize)
                    .limitResultsBefore(Date(beforeMessage.timestamp)).apply {
                        if (conversationId != null) {
                            limitResultsToJid(conversationId.asJid())
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
                                id = displayedMarker.stanzaId,
                                timestamp = forwarded.timestamp(),
                                status = Status.Seen,
                                owner = connection.getOwner(),
                                from = message.getStringFrom(),
                                conversation = connection.getConversationIdFromMessage(message)
                            )
                        )
                    } else {
                        MessageOperation.Created(
                            Message(
                                id = message.stanzaId,
                                body = message.body,
                                timestamp = forwarded.timestamp(),
                                conversation = connection.getConversationIdFromMessage(message),
                                from = message.getStringFrom(),
                                owner = connection.getOwner(),
                                status = Status.Sent,
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


    private suspend fun listenForChatMarkers() = callbackFlow {
        val listener = ChatMarkersListener { state, message, _ ->

            when (state) {
                ChatMarkersState.displayed -> {
                    val displayedMarker = message.getChatMarker()
                    launch {
                        send(
                            MessageOperation.StatusChanged(
                                StatusMessage(
                                    id = displayedMarker.stanzaId,
                                    timestamp = currentTimestamp,
                                    status = Status.Seen,
                                    owner = connection.getOwner(),
                                    from = message.getStringFrom(),
                                    conversation = connection.getConversationIdFromMessage(message)
                                )
                            )
                        )
                    }
                }
                ChatMarkersState.received -> {
                    val receivedMarker = message.getChatMarker()
                    launch {
                        send(
                            MessageOperation.StatusChanged(
                                StatusMessage(
                                    id = receivedMarker.stanzaId,
                                    timestamp = currentTimestamp,
                                    status = Status.Received,
                                    owner = connection.getOwner(),
                                    from = message.getStringFrom(),
                                    conversation = connection.getConversationIdFromMessage(message)
                                )
                            )
                        )
                    }
                }
                else -> { /*Not yet supported*/
                }
            }
        }

        chatMarkersManager.addIncomingChatMarkerMessageListener(listener)

        awaitClose { chatMarkersManager.removeIncomingChatMarkerMessageListener(listener) }
    }

    suspend fun listenForMessageChanges() = merge(listenForChatMarkers(), listenForMessages(), observeAcknowledgmentMessages())

    private suspend fun listenForMessages() = callbackFlow<MessageOperation> {
        val listener = StanzaListener {
            val message = it as? org.jivesoftware.smack.packet.Message

            message?.let {
                when {
                    message.hasExtension(ChatMarkersElements.DisplayedExtension.QNAME) -> {}

                    else -> {
                        launch {
                            message.stanzaId?.let { stanzaId ->
                                send(
                                    MessageOperation.Created(
                                        Message(
                                            id = stanzaId,
                                            body = message.body,
                                            conversation = connection.getConversationIdFromMessage(
                                                message
                                            ),
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
                }
            }
        }

        connection.addStanzaListener(listener) { it is org.jivesoftware.smack.packet.Message }

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
        StanzaFactory { "" }
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
                    if (item.payload is DataExtensionElement) {
                        val payloadItem = item as PayloadItem<DataExtensionElement>
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
                id = message.id,
                timestamp = currentTimestamp,
                status = status,
                owner = connection.getOwner(),
                from = message.from,
                conversation = message.conversation
            )


        val stanza = connection.stanzaFactory
            .buildMessageStanza()
            .to(message.conversation)
            .addExtension(ChatMarkersElements.DisplayedExtension(message.id))
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

    private fun <T> tryExecute(func: () -> T): Resource<T> {
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

    suspend fun createAndSendMessage(message: Message): MessageOperation =
        withContext(Dispatchers.IO) {

            val stanza = connection.stanzaFactory
                .buildMessageStanza()
                .to(message.conversation)
                .ofType(org.jivesoftware.smack.packet.Message.Type.chat)
                .setBody(message.body)
                .addExtension(ChatMarkersElements.MarkableExtension.INSTANCE)
                .build()

            val messageWithId = message.copy(id = stanza.stanzaId)

            val resource = tryExecute {
                connection.sendStanza(stanza)
                waitingForAcknowledgment.update { messageWithId }
            }

            return@withContext when (resource) {
                is Resource.Error -> {
                    Timber.e(resource.cause)
                    MessageOperation.Error(
                        StatusMessage(
                            id = stanza.stanzaId,
                            timestamp = currentTimestamp,
                            status = Status.Error,
                            owner = message.owner,
                            from = message.from,
                            conversation = message.conversation
                        ),
                        resource.cause
                    )
                }
                else -> {
                    MessageOperation.Created(messageWithId)
                }
            }

//            try {
//                connection.addStanzaIdAcknowledgedListener(stanza.stanzaId) {
//                    Timber.d(it.toString())
//                    continuation.resume(
//                        MessageOperation.StatusChanged(
//                            StatusMessage(
//                                id = stanza.stanzaId,
//                                timestamp = currentTimestamp,
//                                status = Status.Sent,
//                                owner = connection.getOwner(),
//                                from = message.from,
//                                conversation = message.conversation
//                            )
//                        )
//                    )
//                }
//            } catch (e: StreamManagementException.StreamManagementNotEnabledException) {
//                Timber.e(e)
//            }

//        awaitClose { connection.removeStanzaIdAcknowledgedListener(stanza.stanzaId) }

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
            addGroupConversationToContacts(
                conversationId = conversationId,
                conversationName = name,
                myNickName = myId,
                onFailure = { Timber.d("Failed to add room", it) }
            )
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

    private suspend fun addGroupConversationToContacts(
        conversationId: String,
        conversationName: String,
        myNickName: String,
        onFailure: (Throwable) -> Unit
    ) {
        run(
            {
                bookmarkManager
                    .addBookmarkedConference(
                        conversationName,
                        conversationId.asJid().asEntityBareJidIfPossible(),
                        true,
                        Resourcepart.from(myNickName),
                        null
                    )
            }
        ) { onFailure(it) }

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