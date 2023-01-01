package com.hanialjti.allchat.data.repository

import androidx.paging.*
import androidx.room.withTransaction
import com.hanialjti.allchat.data.local.room.AllChatLocalRoomDatabase
import com.hanialjti.allchat.data.local.room.entity.MessageEntity
import com.hanialjti.allchat.data.local.room.entity.asNetworkMessage
import com.hanialjti.allchat.data.local.room.model.asMessage
import com.hanialjti.allchat.data.model.*
import com.hanialjti.allchat.data.remote.*
import com.hanialjti.allchat.data.remote.model.*
import com.hanialjti.allchat.data.tasks.MessageTasksDataSource
import com.hanialjti.allchat.presentation.chat.MessageRemoteMediator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.map
import timber.log.Timber

class ChatRepository(
    private val messageTasksDataSource: MessageTasksDataSource,
    private val localDb: AllChatLocalRoomDatabase,
    private val messageRemoteDataSource: MessageRemoteDataSource,
    private val connectionManager: ConnectionManager
) : IChatRepository {

    private val messageDao = localDb.messageDao()
    private val markerDao = localDb.markerDao()
    private val participantDao = localDb.participantDao()


    @OptIn(ExperimentalPagingApi::class)
    override fun messages(conversation: String, owner: String?) = Pager(
        config = PagingConfig(pageSize = 100, enablePlaceholders = true),
        remoteMediator = MessageRemoteMediator(conversation, this),
        pagingSourceFactory = { messageDao.getMessagesByConversation(conversation, owner) }
    ).flow.map { pagingData -> pagingData.map { it.asMessage() } }

    suspend fun resendAllPendingMessages(owner: String) {
        val pendingMessages = messageDao.getPendingMessagesByOwner(owner)
        pendingMessages.forEach {
            messageTasksDataSource.createAndExecuteSendMessageWork(it.id)
        }
    }

    override suspend fun sendSeenMarkerForMessage(externalMessageId: String) =
        withContext(Dispatchers.IO) {
            val message = messageDao.getMessageEntryByRemoteId(externalMessageId)
                ?: return@withContext CallResult.Error("Message not found, can't send marker")

            val sendResult = message.asNetworkMessage()
                ?.let { messageRemoteDataSource.updateMarkerForMessage(it, Marker.Seen) }

            if (sendResult is CallResult.Success) {
                upsertMessage(message.copy(status = MessageStatus.Seen))
                return@withContext CallResult.Success(true)
            } else {
                return@withContext CallResult.Error("Could not send marker")
            }
        }

    override suspend fun setMessagesAsRead(chatId: String) {
        val owner = connectionManager.getUsername()

        if (owner == null) {
            Timber.e("User is not signed in")
        }

        if (owner != null) {
            messageDao.setAllMessagesAsRead(owner, chatId)
        }
    }

    override suspend fun sendMessage(message: MessageEntity) {
        val messageId = messageDao.insertOrReplace(message.copy(read = true))
        message.contactId?.let { setMessagesAsRead(it) }
        if (messageId != -1L) {
            launchSendMessageWork(messageId)
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
            messageRemoteDataSource.getPreviousPage(chatId, oldestMessage?.asNetworkMessage(), pageSize)

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

        message.markers.forEach { (user, marker) ->
            if (messageToUpdate?.contactId != null) {

                if (user != owner) {
                    markerDao.insertMarkersForMessagesBefore(
                        sender = user,
                        marker = marker,
                        timestamp = message.timestamp,
                        owner = owner,
                        chatId = messageToUpdate.contactId
                    )
                }

                val participantCount = participantDao.getParticipantCountForChat(
                    owner,
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
                    handleMessage(message, owner, greatestMessageStatus)
                }
        }

    }

    override suspend fun syncMessages(owner: String) {
        val mostRecentMessage = messageDao.getMostRecentMessage(owner)
        syncMessages(mostRecentMessage?.asNetworkMessage(), 50)
    }

    override suspend fun listenForMessageUpdates() {
        messageRemoteDataSource.listenForMessageChanges()
            .collect { message ->
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
        messageRemoteDataSource.updateMyChatState(chatState)
    }

    override fun observeLastMessageNotSentByOwner(owner: String, conversationId: String) =
        messageDao.getLastMessageNotSendByOwner(owner, conversationId)

    private suspend fun getMessageById(messageId: Int) = messageDao.getMessageById(messageId)

    override suspend fun getMessageByExternalId(externalMessageId: String) =
        messageDao.getMessageByRemoteId(externalMessageId)

}