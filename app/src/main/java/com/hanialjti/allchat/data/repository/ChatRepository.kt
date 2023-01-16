package com.hanialjti.allchat.data.repository

import androidx.paging.*
import androidx.room.withTransaction
import com.hanialjti.allchat.data.local.datastore.UserPreferencesManager
import com.hanialjti.allchat.data.local.room.AllChatLocalRoomDatabase
import com.hanialjti.allchat.data.local.room.entity.MessageEntity
import com.hanialjti.allchat.data.local.room.entity.asMessage
import com.hanialjti.allchat.data.local.room.entity.asNetworkMessage
import com.hanialjti.allchat.data.local.room.entity.getAttachment
import com.hanialjti.allchat.data.model.*
import com.hanialjti.allchat.data.remote.*
import com.hanialjti.allchat.data.remote.model.*
import com.hanialjti.allchat.data.tasks.MessageTasksDataSource
import com.hanialjti.allchat.presentation.chat.MessageRemoteMediator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.datetime.*
import timber.log.Timber

class ChatRepository(
    private val messageTasksDataSource: MessageTasksDataSource,
    private val localDb: AllChatLocalRoomDatabase,
    private val messageRemoteDataSource: MessageRemoteDataSource,
    private val connectionManager: ConnectionManager,
    private val preferencesManager: UserPreferencesManager,
    private val externalScope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher
) : IChatRepository {

    private val messageDao = localDb.messageDao()
    private val userDao = localDb.userDao()
    private val markerDao = localDb.markerDao()
    private val participantDao = localDb.participantDao()
    private val conversationDao = localDb.conversationDao()


    @OptIn(ExperimentalPagingApi::class)
    private fun messages(conversation: String, owner: String?) = Pager(
        config = PagingConfig(pageSize = 100, enablePlaceholders = true),
        remoteMediator = MessageRemoteMediator(conversation, this),
        pagingSourceFactory = { messageDao.getMessagesByConversation(conversation, owner) }
    ).flow.map { pagingData -> pagingData.map { it.asMessage() } }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun fetchMessagesFor(chatId: String) = preferencesManager.loggedInUser
        .flatMapLatest { it?.let { messages(chatId, it) } ?: emptyFlow() }

    suspend fun resendAllPendingMessages(owner: String) {
        val pendingMessages = messageDao.getPendingMessagesByOwner(owner)
        pendingMessages.forEach {
            messageTasksDataSource.createAndExecuteSendMessageWork(it.id)
        }
    }

    override suspend fun sendSeenMarkerForMessage(externalMessageId: String) =
        externalScope.async(dispatcher) {
            val message = messageDao.getMessageEntryByRemoteId(externalMessageId)
                ?: return@async CallResult.Error("Message not found, can't send marker")

            val sendResult = message.asNetworkMessage()
                ?.let { messageRemoteDataSource.updateMarkerForMessage(it, Marker.Seen) }

            if (sendResult is CallResult.Success) {
                upsertMessage(message.copy(status = MessageStatus.Seen))
                return@async CallResult.Success(true)
            } else {
                return@async CallResult.Error("Could not send marker")
            }
        }.await()

    override suspend fun setMessagesAsRead(chatId: String) {
        externalScope.launch {
            val owner = connectionManager.getUsername()

            if (owner == null) {
                Timber.e("User is not signed in")
            }

            if (owner != null) {
                messageDao.setAllMessagesAsRead(owner, chatId)
            }
        }
    }

    override suspend fun sendMessage(message: MessageEntity) {
        externalScope.launch {
            val messageId = messageDao.insertOrReplace(message.copy(read = true))
            message.contactId?.let {
                conversationDao.updateLastMessage(
                    MessageSummary(
                        body = message.body,
                        status = MessageStatus.Pending,
                        timestamp = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                        attachmentType = message.getAttachment()?.type,
                        isSentByMe = true
                    ),
                    it
                )
            }
            message.contactId?.let { setMessagesAsRead(it) }
            if (messageId != -1L) {
                launchSendMessageWork(messageId)
            }
        }
    }

    override suspend fun sendMessageAndRegisterForAcknowledgment(messageId: Int): CallResult<String> {
        val messageToSend = getMessageById(messageId)
            ?: return CallResult.Error("message not found", null)

        val sendResult = messageRemoteDataSource.sendMessage(
            messageToSend,
            connectionManager.getConfig().chatMarkersEnabled
        )

        if (sendResult is CallResult.Success) {
            val externalId = sendResult.data ?: return CallResult.Error("messageId is null")
            upsertMessage(
                messageToSend.copy(externalId = externalId)
            )
        }

        return sendResult
    }

    private fun launchSendMessageWork(messageId: Long) {
        messageTasksDataSource.createAndExecuteSendMessageWork(messageId)
    }

    override suspend fun retrievePreviousPage(
        chatId: String,
        oldestMessage: MessageEntity?,
        pageSize: Int
    ) = withContext(Dispatchers.IO) {
        val owner = connectionManager.getUsername()
            ?: return@withContext MessageQueryResult.Error(SecurityException("User is not signed in"))

        val status =
            oldestMessage?.contactId?.let { messageDao.getGreatestMessageStatus(it, owner) }
        val messagePage =
            messageRemoteDataSource.getPreviousPage(
                chatId,
                oldestMessage?.asNetworkMessage(),
                pageSize
            )

        saveMessagePage(messagePage, status)

        return@withContext messagePage.error?.let {
            MessageQueryResult.Error(messagePage.error)
        } ?: MessageQueryResult.Success(messagePage.isComplete)
    }

    override suspend fun retrieveNextPage(
        chatId: String,
        newestMessage: MessageEntity?,
        pageSize: Int
    ) = withContext(Dispatchers.IO) {

        val messagePage =
            messageRemoteDataSource.getNextPage(chatId, newestMessage?.asNetworkMessage(), pageSize)

        saveMessagePage(messagePage)

        return@withContext messagePage.error?.let {
            MessageQueryResult.Error(messagePage.error)
        } ?: MessageQueryResult.Success(messagePage.isComplete)
    }

    private suspend fun syncMessages(
        afterMessage: RemoteMessage?,
        pageSize: Int
    ) = withContext(Dispatchers.IO) {
        val messagePage = messageRemoteDataSource.syncMessages(afterMessage, pageSize)

        saveMessagePage(messagePage)

        return@withContext messagePage.error?.let {
            MessageQueryResult.Error(messagePage.error)
        } ?: MessageQueryResult.Success(messagePage.isComplete)
    }

    private suspend fun handleMessage(
        message: RemoteMessage,
        owner: String,
        greatestMessageStatus: MessageStatus?
    ) {
        upsertMessage(message.asMessageEntity().copy(ownerId = owner))

        val messageToUpdate = messageDao.getMessageByRemoteId(message.id)

        messageToUpdate?.contactId?.let {
            conversationDao.updateLastMessage(
                MessageSummary(
                    body = messageToUpdate.body,
                    status = messageToUpdate.status,
                    timestamp = messageToUpdate.timestamp.toLocalDateTime().toKotlinLocalDateTime(),
                    attachmentType = messageToUpdate.getAttachment()?.type,
                    isSentByMe = messageToUpdate.senderId == owner
                ),
                it
            )
        }

        message.markers.forEach { (userId, marker) ->
            if (messageToUpdate?.contactId != null) {
                val userExists = userDao.exists(userId)
                if (userId != owner && userExists) {
                    markerDao.insertMarkersForMessagesBefore(
                        sender = userId,
                        marker = marker,
                        timestamp = message.timestamp,
                        owner = owner,
                        chatId = messageToUpdate.contactId
                    )
                }

                val participantCount = participantDao.getParticipantCountForChat(
                    messageToUpdate.contactId
                )
                val markerCount = markerDao.getCountForMarker(
                    messageId = message.id,
                    marker = marker
                )
                if (participantCount == markerCount) {
                    val highestMarker = greatestMessageStatus?.let {
                        MessageStatus.max(
                            it,
                            marker.toMessageStatus()
                        )
                    } ?: marker.toMessageStatus()
                    messageDao.updateStatusForMessagesBeforeTimestamp(
                        highestMarker,
                        message.timestamp,
                        owner,
                        messageToUpdate.contactId
                    )

                }
            }

        }
    }

    private suspend fun saveMessagePage(
        messagePage: MessagePage,
        greatestMessageStatus: MessageStatus? = null
    ) {
        val owner = connectionManager.getUsername()

        if (owner == null) {
            Timber.e("User is not signed in")
            return
        }

        localDb.withTransaction {
            messagePage
                .messageList
                .onEach { message ->
                    when (message) {
                        is RemoteMessage -> {
                            handleMessage(message, owner, greatestMessageStatus)
                        }
                        is RemoteGroupInvitation -> {

                        }
                    }
                }
        }

    }

    override suspend fun syncMessages(owner: String) {
        val mostRecentMessage = messageDao.getMostRecentMessage(owner)
        syncMessages(mostRecentMessage?.asNetworkMessage(), 50)
    }

    private fun handleGroupChatInvitation(invitation: RemoteGroupInvitation) {

    }

    override suspend fun listenForMessageUpdates() {
        messageRemoteDataSource.listenForMessageChanges()
            .collect { message ->

                when (message) {
                    is RemoteMessage -> {
                        Timber.d("received new message $message")

                        localDb.withTransaction {

                            val owner = connectionManager.getUsername()

                            if (owner == null) {
                                Timber.e("User is not signed in. Received messages will not be saved")
                                return@withTransaction
                            }

                            handleMessage(message, owner, null)

                            message.chatId?.let { setMessagesAsRead(it) }

                        }
                    }
                    is RemoteGroupInvitation -> {

                    }
                }

            }
    }

//    suspend fun observeChatStates() {
//        messageRemoteDataSource.observeChatStates()
//            .collect { chatState ->
//                val conversation =
//                    conversationsDao.getConversationByRemoteId(chatState.conversation)
//                conversation?.let {
////                    conversationsDao.update(
////                        it.copy(states = it.states.apply { put(chatState.from, chatState.state) })
////                    )
//                }
//            }
//    }

    private suspend fun upsertMessage(message: MessageEntity) {
        messageDao.upsertMessage(message)
    }

    override suspend fun updateMyChatState(chatState: ChatState) {
//        messageRemoteDataSource.updateMyChatState(chatState)
    }

    override fun observeLastMessageNotSentByOwner(owner: String, conversationId: String) =
        messageDao.getLastMessageNotSendByOwner(owner, conversationId)

    private suspend fun getMessageById(messageId: Int) = messageDao.getMessageById(messageId)

    override suspend fun getMessageByExternalId(externalMessageId: String) =
        messageDao.getMessageByRemoteId(externalMessageId)


}