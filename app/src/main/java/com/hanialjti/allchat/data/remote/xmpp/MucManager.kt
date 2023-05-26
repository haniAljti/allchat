package com.hanialjti.allchat.data.remote.xmpp

import com.hanialjti.allchat.common.utils.Logger
import com.hanialjti.allchat.common.utils.suspendCoroutineWithTimeout
import com.hanialjti.allchat.data.model.MessageStatus
import com.hanialjti.allchat.data.model.MessageType
import com.hanialjti.allchat.data.model.Role
import com.hanialjti.allchat.data.remote.model.*
import com.hanialjti.allchat.data.remote.xmpp.model.*
import com.hanialjti.allchat.data.remote.xmpp.model.ChatMarkerWrapper.Companion.toMarker
import com.hanialjti.allchat.data.remote.xmpp.model.ChatMarkerWrapper.Companion.toMessageStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jivesoftware.smack.StanzaListener
import org.jivesoftware.smack.filter.*
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smackx.bookmarks.BookmarkManager
import org.jivesoftware.smackx.hints.element.NoStoreHint
import org.jivesoftware.smackx.muc.MUCAffiliation
import org.jivesoftware.smackx.muc.MultiUserChat
import org.jivesoftware.smackx.muc.MultiUserChatException
import org.jivesoftware.smackx.muc.MultiUserChatManager
import org.jivesoftware.smackx.muc.packet.GroupChatInvitation
import org.jivesoftware.smackx.muc.packet.MUCInitialPresence
import org.jivesoftware.smackx.muc.packet.MUCUser
import org.jivesoftware.smackx.pep.PepEventListener
import org.jivesoftware.smackx.pep.PepManager
import org.jivesoftware.smackx.pubsub.AccessModel
import org.jivesoftware.smackx.pubsub.PayloadItem
import org.jivesoftware.smackx.pubsub.PubSubManager
import org.jxmpp.jid.parts.Resourcepart
import timber.log.Timber
import java.util.*
import kotlin.coroutines.resume

class MucManager(
    private val connection: XMPPTCPConnection,
    private val clientDataStore: XmppClientDataSource,
    externalScope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    private val mucManager: MultiUserChatManager = MultiUserChatManager.getInstanceFor(connection),
    private val bookmarkManager: BookmarkManager = BookmarkManager.getBookmarkManager(connection),
    private val pepManager: PepManager = PepManager.getInstanceFor(connection),
) {

    private val roomsInfoState = mutableMapOf<String, RoomState>()
    private var hasJoinedAllRooms = false

    private fun subjectListener(onSubjectChange: (roomId: String, subject: String) -> Unit) =
        StanzaListener { stanza ->
            val message = stanza as Message
            val chatId = message.fromAsString() ?: return@StanzaListener
            val subject = message.subject
            Logger.d { "New Subject for '$chatId': '$subject'" }

            if (!subject.isNullOrBlank()) onSubjectChange(chatId, subject)
        }

    private fun messageListener(onMessage: (RemoteMessage) -> Unit) = StanzaListener { stanza ->

        val message = stanza as Message

        if (message.body != null || message.hasExtension(OutOfBandData.NAMESPACE)) {
            val chatMarker = message.wrapMarker()
            val chatId = message.fromAsString() ?: return@StanzaListener
            val from = message.from?.resourceOrNull
                ?.toString()
                ?.plus("@")
                ?.plus(connection.xmppServiceDomain.domain.toString())
                ?: return@StanzaListener

            onMessage(
                RemoteMessage(
                    id = chatMarker?.stanzaId ?: message.stanzaId,
                    body = if (message.isMessage()) message.body else null,
                    chatId = chatId,
                    sender = from,
                    attachment = message.extractAttachment(),
                    type = MessageType.GroupChat,
                    thread = message.thread,
                    messageStatus = chatMarker?.toMessageStatus()
                        ?: MessageStatus.Sent,
                    markers = chatMarker?.toMarker()?.let { mutableMapOf(from to it) }
                        ?: mutableMapOf()
                )
            )
        }
    }

    private fun roomConfigurationChangeListener(
        onConfigurationChange: (chatId: String) -> Unit
    ) = StanzaListener { stanza ->

        val chatId = stanza.fromAsString() ?: return@StanzaListener

        val mucUser = MUCUser.from(stanza) ?: return@StanzaListener
        if (!mucUser.hasStatus()) return@StanzaListener

        mucUser.status.forEach {
            if (it == MUCUser.Status.create(104) || it == MUCUser.Status.create(170)) {
                Logger.d { "Configuration changed event received" }
                onConfigurationChange(chatId)
            }

        }
    }

    private fun presenceListener(
        onPresence: (chatId: String, updatedParticipantState: RemoteParticipant) -> Unit
    ) = StanzaListener { packet ->

        if (!packet.hasExtension(MUCUser.NAMESPACE) && !packet.hasExtension(MUCInitialPresence.NAMESPACE))
            return@StanzaListener // not a muc presence

        Logger.d { "New MUC Presence Packet $packet" }
        val presence = packet as org.jivesoftware.smack.packet.Presence
        val participant = presence.from?.resourceOrNull?.toString()
            ?.plus("@${connection.xmppServiceDomain.domain}")
            ?: return@StanzaListener

        val mucUser = MUCUser.from(presence)
        val room = presence.fromAsString() ?: return@StanzaListener

        val affiliation = mucUser?.item?.affiliation ?: return@StanzaListener

        val newParticipant = when (affiliation) {
            MUCAffiliation.member -> {
                RemoteParticipant(participant, Role.Participant)
            }

            MUCAffiliation.owner -> {
                RemoteParticipant(participant, Role.Owner)
            }

            MUCAffiliation.admin -> {
                RemoteParticipant(participant, Role.Admin)
            }

            else -> {
                RemoteParticipant(participant, Role.None)
            }
        }

        onPresence(room, newParticipant)

    }

    fun getRoomSubject(chatId: String) = roomsInfoState[chatId]?.subject

    val roomAdditionStream = merge(
        bookmarkedConferencesStream(),
        listenForGroupInvitations()
    )

    private fun bookmarkedConferencesStream() = callbackFlow {

        val listener = PepEventListener<MucBookmark> { _, _, id, carrierMessage ->
            Logger.d { carrierMessage.toString() }
            Logger.d { "New MucBookmark.." }
            launch {

                val oldState = clientDataStore
                    .chatRooms
                    .first()
                    .find { it.id == id }

                val newState = tryTofetchAndUpdateRoomState(id)

                if (oldState != newState) {
                    clientDataStore.updateChatRoomState(newState)
                    send(MucStateUpdate(newState))
                }

//                clientDataStore.addChatRooms(RoomState(id))
//                send(
//                    OneOnOneChatAdded(id, true)
//                )
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

    private fun listenForGroupInvitations() = callbackFlow {

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
                    val oldState = clientDataStore
                        .chatRooms
                        .first()
                        .find { it.id == roomAddress }

                    val newState = tryTofetchAndUpdateRoomState(roomAddress)

                    if (oldState != newState) {
                        clientDataStore.updateChatRoomState(newState)
                        send(MucStateUpdate(newState))
                    }
//                    clientDataStore.addChatRooms(RoomState(roomAddress))
//                    send(
//                        OneOnOneChatAdded(roomAddress, true)
//                    )
                }
            }
        }
        connection.addStanzaListener(invitationListener, invitationFilter)
        awaitClose { connection.removeStanzaListener(invitationListener) }
    }

    private suspend fun tryTofetchAndUpdateRoomState(chatId: String): RoomState {
        val roomInfo = getRoomInfo(chatId)

        val oldState = clientDataStore
            .chatRooms
            .first()
            .find { it.id == chatId }

        return if (roomInfo is CallResult.Success) {

            oldState?.copy(
                description = roomInfo.data?.description,
                avatarUrl = roomInfo.data?.avatarUrl,
                participants = roomInfo.data?.participants ?: mapOf(),
                createdAt = roomInfo.data?.createdAt
            ) ?: RoomState(
                id = chatId,
                description = roomInfo.data?.description,
                avatarUrl = roomInfo.data?.avatarUrl,
                participants = roomInfo.data?.participants ?: mapOf(),
                createdAt = roomInfo.data?.createdAt
            )

        } else oldState ?: RoomState(chatId).apply {
            clientDataStore.addChatRooms(this)
        }
    }

    val roomChangesStream = callbackFlow {

        fun updateAndEmitChatRoomState(chatId: String) {
            launch {

                delay(500) // wait for initial data through the listeners to emit

                val oldState = clientDataStore
                    .chatRooms
                    .first()
                    .find { it.id == chatId }

                val newState = tryTofetchAndUpdateRoomState(chatId)

                if (oldState != newState) {
                    clientDataStore.updateChatRoomState(newState)
                    send(MucStateUpdate(newState))
                }
            }
        }

        val subjectListener = subjectListener { chatId, subject ->
            launch {
                val oldState = clientDataStore
                    .chatRooms
                    .first()
                    .find { it.id == chatId }

                if (subject.isNotBlank()) {
                    val newState = oldState?.copy(subject = subject) ?: RoomState(
                        id = chatId,
                        subject = subject
                    )

                    if (newState != oldState) {
                        clientDataStore.updateChatRoomState(newState)
                        send(MucStateUpdate(newState))
                    }
                }
            }
        }


        Logger.d { "Registering MUC subject listener" }
        connection.addStanzaListener(
            subjectListener,
            AndFilter(
                MessageTypeFilter.GROUPCHAT,
                MessageWithSubjectFilter.INSTANCE,
                NotFilter(MessageTypeFilter.ERROR),
                // According to XEP-0045 § 8.1 "A message with a <subject/> and a <body/> or a <subject/> and a <thread/> is a
                // legitimate message, but it SHALL NOT be interpreted as a subject change."
                NotFilter(MessageWithBodiesFilter.INSTANCE),
                NotFilter(MessageWithThreadFilter.INSTANCE)
            )
        )

        val messageListener = messageListener { remoteMessage ->
            launch { send(MucMessageUpdate(remoteMessage)) }
        }

        Logger.d { "Registering MUC message listener" }
        connection.addStanzaListener(
            messageListener,
            MessageTypeFilter.GROUPCHAT
        )

        val roomConfigurationChangeListener = roomConfigurationChangeListener {
            updateAndEmitChatRoomState(it)
        }

        Logger.d { "Registering MUC configuration change listener" }
        connection.addStanzaListener(
            roomConfigurationChangeListener,
            MessageTypeFilter.GROUPCHAT
        )

        val participantChangeListener = presenceListener { chatId, updatedParticipantState ->
            launch {
                val oldState = clientDataStore
                    .chatRooms
                    .first()
                    .find { it.id == chatId } ?: return@launch

                val participantId = updatedParticipantState.id

                val oldParticipant = oldState
                    .participants[participantId]

                if (oldParticipant == null || oldParticipant != updatedParticipantState) {
                    val newState = oldState.copy(
                        participants = oldState
                            .participants
                            .toMutableMap()
                            .apply {

                                if (containsKey(participantId) && oldParticipant?.role!! < updatedParticipantState.role) {
                                    remove(participantId); this[participantId] =
                                        updatedParticipantState
                                }
                            }
                    )
                    if (newState != oldState) {
                        clientDataStore.updateChatRoomState(newState)
                        send(MucStateUpdate(newState))
                    }
                }
            }
        }

        Logger.d { "Registering MUC participant listener" }
        connection.addStanzaListener(
            participantChangeListener,
            AndFilter(
                StanzaTypeFilter.PRESENCE,
                PossibleFromTypeFilter.ENTITY_FULL_JID,
            )
        )


        if (!hasJoinedAllRooms) {


            clientDataStore
                .isAuthenticated
                .collectLatest {

                    if (it) {

                        val allRooms = retrieveGroupChats()
                        allRooms.forEach { chatRoom ->
                            clientDataStore.addChatRooms(RoomState(chatRoom.id))
                            if (!hasJoinedAllRooms) {
                                joinRoom(chatRoom.id)
                                updateAndEmitChatRoomState(chatRoom.id)
                            }
                        }
                        hasJoinedAllRooms = true
                    }

                }
        }


        awaitClose {
            Logger.d { "Removing MUC subject listener" }
            connection.removeStanzaListener(subjectListener)
            Logger.d { "Removing MUC message listener" }
            connection.removeStanzaListener(messageListener)
            Logger.d { "Removing MUC participant listener" }
            connection.removeStanzaListener(participantChangeListener)
            Logger.d { "Removing MUC configuration change listener" }
            connection.removeStanzaListener(roomConfigurationChangeListener)
        }

    }.shareIn(
        scope = externalScope,
        started = SharingStarted.WhileSubscribed(),
        replay = 1000
    )


    suspend fun getRoomInfo(roomId: String): CallResult<RemoteRoomInfo> {
        return try {
            val muc = mucManager.getMultiUserChat(roomId.asEntityBareJid())
            val mucRoomInfo =
                mucManager.getRoomInfo(roomId.asJid().asEntityBareJidIfPossible())
            val roomInfo = try {
                mucRoomInfo?.description?.let { Json.decodeFromString<RoomInfo>(it) }
            } catch (e: SerializationException) {
                RoomInfo(mucRoomInfo?.description, null, null, null)
            }

            val allMembers = mutableMapOf<String, RemoteParticipant>()
            muc.members
                .forEach {
                    val participant = it.jid.asBareJid().toString()
                    allMembers[participant] = RemoteParticipant(
                        id = it.jid.asBareJid().toString(),
                        role = Role.Participant
                    )
                }
            muc.admins
                .forEach {
                    val participant = it.jid.asBareJid().toString()
                    allMembers[participant] = RemoteParticipant(
                        id = it.jid.asBareJid().toString(),
                        role = Role.Admin
                    )
                }
            muc.owners
                .forEach {
                    val participant = it.jid.asBareJid().toString()
                    allMembers[participant] = RemoteParticipant(
                        id = it.jid.asBareJid().toString(),
                        role = Role.Owner
                    )
                }
            CallResult.Success(
                RemoteRoomInfo(
                    description = roomInfo?.description,
                    avatarUrl = roomInfo?.avatarUrl,
                    participants = allMembers,
                    createdAt = mucRoomInfo.getCreationDate(),
                    createdBy = allMembers.values.first { it.role == Role.Owner }.id
                )
            )
        } catch (e: Exception) {
            CallResult.Error(message = "No response from server")
        }
    }

    suspend fun getRoomDescription(chatId: String): CallResult<RoomInfo?> =
        withContext(dispatcher) {
            val description = try {
                val roomInfo =
                    mucManager.getRoomInfo(chatId.asJid().asEntityBareJidIfPossible())
                if (roomInfo.description.isNullOrBlank()) null else roomInfo.description
            } catch (e: Exception) {
                Logger.e(e)
                null
            }

            return@withContext try {
//                joinRoom(chatId)
                CallResult.Success(description?.let { Json.decodeFromString(it) })
            } catch (e: SerializationException) {
                CallResult.Success(RoomInfo(description, null, null, null))
            } catch (e: Exception) {
                Logger.e(e)
                CallResult.Error("Error while fetching room description")
            }
        }

    suspend fun retrieveGroupChats() = withContext(dispatcher) {

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
            .map { RemoteChat(it, true) }
            .toSet()
    }

    private suspend fun joinRoom(roomId: String) =
        try {
            Logger.d { "Trying to join '$roomId'..." }
            val muc =
                mucManager.getMultiUserChat(roomId.asEntityBareJid())
            val myNickname = connection.user.localpartOrThrow.toString()
            if (!muc.isJoined) {
                Logger.d { "Joining $roomId..." }
                val history = muc
                    .getEnterConfigurationBuilder(Resourcepart.from(myNickname))
                    .build()
                muc.join(history)
            }
            true
        } catch (e: Exception) {
            Logger.e(e)
            false
        }

    private suspend fun String.asEntityBareJid() =
        asJid().asEntityBareJidIfPossible()

    suspend fun updateChatInfo(
        chatId: String,
        description: String?,
        avatarUrl: String?,
        subject: String?
    ): CallResult<Boolean> {

        val oldState = clientDataStore
            .chatRooms
            .first()
            .find { it.id == chatId }
            ?: RoomState(
                id = chatId,
                description = description,
                avatarUrl = avatarUrl,
                subject = subject
            ).also { clientDataStore.addChatRooms(it) }

        val newSubject = (subject ?: oldState.subject)
            ?: return CallResult.Error("Subject can not be null")

        val descSetResult =
            if (description != oldState.description || avatarUrl != oldState.avatarUrl) {
                val newState = RoomInfo(
                    description = if (oldState.description != description) description else oldState.description,
                    avatarUrl = if (oldState.avatarUrl != avatarUrl) avatarUrl else oldState.avatarUrl,
                    createdAt = null,
                    createdBy = null
                )

                setRoomDesc(chatId, Json.encodeToString(newState))
            } else CallResult.Success()

        if (descSetResult is CallResult.Error)
            return CallResult.Error("Could not set room info")


        //send always even when not changed to let clients know that the room configuration
        //has changes since not all servers send a notification (like openfire)
        updateRoomSubject(chatId, newSubject)


//            return@withContext try {
//                withTimeout(1000) {
//                    suspendCancellableCoroutine<CallResult<Boolean>> { continuation ->
//                        launch {
//                            try {
//                                val listener = StanzaListener { stanza ->
//
//                                    val mucUser = MUCUser.from(stanza) ?: return@StanzaListener
//                                    if (!mucUser.hasStatus()) return@StanzaListener
//
//                                    mucUser.status.forEach {
//                                        if (it == MUCUser.Status.create(104) || it == MUCUser.Status.create(
//                                                170
//                                            )
//                                        ) {
//                                            continuation.resume(CallResult.Success(true))
//                                        }
//                                    }
//
//                                }
//
//                                connection.addStanzaListener(listener, MessageTypeFilter.GROUPCHAT)
//
//                                mucManager
//                                    .getMultiUserChat(chatId.asEntityBareJid())
//                                    .configFormManager
//                                    .setRoomDesc(Json.encodeToString(newState))
//                                    .submitConfigurationForm()
//
//                                continuation.invokeOnCancellation {
//                                    connection.removeStanzaListener(listener)
//                                }
//
//                            } catch (e: Exception) {
//                                Logger.e(e)
//                                continuation.resume(
//                                    CallResult.Error(
//                                        "An error occurred while updating user data", e
//                                    )
//                                )
//                            }
//                        }
//                    }
//                }
//            } catch (e: TimeoutCancellationException) {
//                // Configuration change update is not sent by the server
//                sendMessageToRoom(
//                    chatId,
//                    connection.stanzaFactory
//                        .buildMessageStanza()
//                        .addExtension(NoStoreHint.INSTANCE)
//                        .addExtension(MUCUser().apply { addStatusCode(MUCUser.Status.create(104)) })
//                        .build()
//                )
//                return@withContext CallResult.Success(true)
//            }
//
//        }
//
//        mucManager
//            .getMultiUserChat(chatId.asEntityBareJid())
//            .configFormManager
//            .setRoomDesc(Json.encodeToString(newState))
//            .submitConfigurationForm()

        return CallResult.Success(true)
    }

    @Throws(MultiUserChatException.MucConfigurationNotSupportedException::class)
    private suspend fun setRoomDesc(chatId: String, description: String?): CallResult<Boolean> {
        return try {
            val muc = mucManager.getMultiUserChat(chatId.asEntityBareJid())
            val form = muc.configurationForm.fillableForm
            if (form.hasField("muc#roomconfig_roomdesc")) {
                form.setAnswer("muc#roomconfig_roomdesc", description)
                muc.sendConfigurationForm(form)
                CallResult.Success(true)
            } else CallResult.Error("Room description is not supported by the server!")
        } catch (e: Exception) {
            Logger.e(e)
            CallResult.Error("An error occurred while updating room description")
        }
    }

    suspend fun updateRoomSubject(chatId: String, subject: String) {
        try {
            val muc = mucManager.getMultiUserChat(chatId.asEntityBareJid())
            muc.changeSubject(subject)
        } catch (e: Exception) {
            Logger.e(e)
        }
    }

    suspend fun createChatRoom(
        roomName: String,
        invitees: Set<String>
    ): CallResult<String> = withContext(dispatcher) {

        if (!connection.isAuthenticated) {
            return@withContext CallResult.Error("User is not authenticated")
        }

        val roomId = UUID.randomUUID().toString()

        val roomJid = try {
            roomId.plus("@").plus(mucManager.mucServiceDomains.first())
        } catch (e: Exception) {
            Logger.e(e)
            null
        }

        val muc = try {
            mucManager.getMultiUserChat(roomJid?.asJid()?.asEntityBareJidIfPossible())
        } catch (e: Exception) {
            Logger.e(e)
            null
        }

        val myId = connection.user.asBareJid().localpartOrNull?.toString()

        return@withContext try {

            muc?.apply {
                create(Resourcepart.from(myId))
                    .configFormManager
                    .makeMembersOnly()
                    .submitConfigurationForm()

                invitees.forEach {
                    grantMembership(it.asEntityBareJid())
                    muc.inviteDirectly(it.asJid().asEntityBareJidIfPossible())
                }
                changeSubject(roomName)
            }

            if (roomJid != null) {
                bookmarkConference(roomJid, roomName)

                clientDataStore.addChatRooms(
                    RoomState(
                        id = roomJid,
                        createdBy = myId,
                        subject = roomName
                    )
                )
            }

            CallResult.Success(roomJid)
        } catch (e: Exception) {
            Timber.e(e)
            muc?.destroy("Error while creating the room", null)
            if (roomJid != null) {
                clientDataStore.removeChatRooms(roomJid)
            }
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
                val bookmarksNode =
                    pubSub.getOrCreateLeafNode(MucBookmark.NAMESPACE)

                val nodeConfiguration = bookmarksNode.nodeConfiguration
                val fillableForm = nodeConfiguration.fillableForm

                fillableForm.apply {
                    setPersistentItems(true)
                    accessModel = AccessModel.whitelist
                    if (nodeConfiguration.hasField("pubsub#send_last_published_item")) {
                        setAnswer("pubsub#send_last_published_item", "never")
                    }
                    maxItems =
                        if (nodeConfiguration.maxItems < 50) 1000 else nodeConfiguration.maxItems
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