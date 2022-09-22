package com.hanialjti.allchat.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.work.ListenableWorker
import com.hanialjti.allchat.AllChatWorkManager
import com.hanialjti.allchat.localdatabase.AllChatLocalRoomDatabase
import com.hanialjti.allchat.models.ChatState
import com.hanialjti.allchat.models.MessageOperation
import com.hanialjti.allchat.models.MessageQueryResult
import com.hanialjti.allchat.models.Resource
import com.hanialjti.allchat.models.entity.Message
import com.hanialjti.allchat.models.entity.Status
import com.hanialjti.allchat.models.entity.StatusMessage
import com.hanialjti.allchat.models.entity.toErrorStatusMessage
import com.hanialjti.allchat.paging.MessageRemoteMediator
import com.hanialjti.allchat.xmpp.XmppConnectionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class XmppChatRepository constructor(
    private val workManager: AllChatWorkManager,
    localDb: AllChatLocalRoomDatabase,
    private val remoteDb: XmppConnectionHelper,
) {

    private val messageDao = localDb.messageDao()
    private val conversationsDao = localDb.conversationDao()

    @OptIn(ExperimentalPagingApi::class)
    fun messages(conversation: String, owner: String?) = Pager(
        config = PagingConfig(pageSize = 100),
        remoteMediator = MessageRemoteMediator(conversation, this),
        pagingSourceFactory = { messageDao.getMessagesByConversation(conversation, owner) }
    ).flow

    suspend fun resendAllPendingMessages(owner: String) {
        val pendingMessages = messageDao.getPendingMessagesByOwner(owner)
        pendingMessages.forEach {
            workManager.createAndExecuteSendMessageWork(it.id.toLong())
        }
    }

    suspend fun markMessageAsDisplayed(messageId: Int) = withContext(Dispatchers.IO) {
        val message = getMessageById(messageId)
        val sendResult = remoteDb.markMessageAsDisplayed(message)
        handleMessageOperation(sendResult)
    }

    suspend fun sendMessage(message: Message) {
        val messageId = messageDao.insertOrIgnore(message)
        if (messageId != -1L) {
            launchSendMessageWork(messageId)
        }
    }

    suspend fun sendMessageAndRegisterForAcknowledgment(messageId: Int): Resource<Message?> {
        val messageToSend = getMessageById(messageId)

        val sendResult = remoteDb.sendMessage(messageToSend)

        when (sendResult) {
            is Resource.Success -> {
                sendResult.data?.let { updateMessage(it) }
            }
            is Resource.Error -> {
                updateMessage(messageToSend.copy(status = Status.Error))
            }
            else -> {  }
        }

        return sendResult
    }

    private fun launchSendMessageWork(messageId: Long) {
        workManager.createAndExecuteSendMessageWork(messageId)
    }

    suspend fun retrievePreviousPage(
        beforeMessage: Message?,
        conversationId: String,
        pageSize: Int
    ) = withContext(Dispatchers.IO) {
        val messagePage = remoteDb.getPreviousPage(beforeMessage, conversationId, pageSize)
        messagePage
            .messageList
            .forEach {
                handleMessageOperation(it)
            }
        return@withContext messagePage.error?.let {
            MessageQueryResult.Error(messagePage.error)
        } ?: MessageQueryResult.Success(messagePage.isComplete)

    }

    suspend fun syncMessages(
        afterMessage: Message?,
        pageSize: Int
    ) = withContext(Dispatchers.IO) {
        val messagePage = remoteDb.syncMessages(afterMessage, pageSize)
        messagePage
            .messageList
            .forEach {
                handleMessageOperation(it)
            }
        return@withContext messagePage.error?.let {
            MessageQueryResult.Error(messagePage.error)
        } ?: MessageQueryResult.Success(messagePage.isComplete)
    }

    suspend fun syncMessages(owner: String) {
        val mostRecentMessage = messageDao.getMostRecentMessage(owner)

        syncMessages(mostRecentMessage, 50)
    }

    suspend fun listenForMessageChanges() {
        remoteDb.listenForMessageChanges()
            .collect { message ->
                Timber.d("registering new message $message")
                handleMessageOperation(message)
            }
    }

    private suspend fun updatePreviousMessagesStatus(message: Message) {
        if (message.conversation != null && message.owner != null) {
            messageDao.updateStatusForMessagesBeforeTimestamp(
                Status.Seen,
                message.timestamp,
                message.owner,
                message.conversation
            )
        }
    }

    suspend fun observeChatStates() {
        remoteDb.observeChatStates()
            .collect { chatState ->
                val conversation = conversationsDao.getConversationById(chatState.conversation)
                conversation?.let {
                    conversationsDao.update(
                        it.copy(states = it.states.apply { put(chatState.from, chatState.state) })
                    )
                }
            }
    }

    private suspend fun updateMessage(message: Message) {
        messageDao.updateMessage(message)
    }

    suspend fun upsertMessage(message: Message) {
        messageDao.upsertMessage(message)
        message.conversation?.let {
            conversationsDao.updateLastMessage(
                lastMessage = message.body,
                lastUpdated = message.timestamp,
                conversationId = it
            )
        }
    }

    private suspend fun upsertStatusChange(statusMessage: StatusMessage) {
        messageDao.upsertMessageStatus(statusMessage)
    }

    suspend fun handleMessageOperation(messageOperation: MessageOperation) {
        when (messageOperation) {
            is MessageOperation.Created -> {
                upsertMessage(messageOperation.message)
            }
            is MessageOperation.StatusChanged -> {
                Timber.d("${messageOperation.message.remoteId} is now ${messageOperation.message.status}")
                upsertStatusChange(messageOperation.message)
                if (messageOperation.message.status == Status.Seen && messageOperation.message.owner != messageOperation.message.from) {
                    val message = messageDao.getMessageByRemoteId(messageOperation.id)
                    if (message != null) {
                        updatePreviousMessagesStatus(message)
                    }
                }
            }
            is MessageOperation.Error -> {
                Timber.e(messageOperation.cause)
                upsertStatusChange(messageOperation.message)
            }
            else -> {}
        }
    }

    suspend fun updateMyChatState(chatState: ChatState) {
        remoteDb.updateMyChatState(chatState)
    }

    fun observeLastMessageNotSentByOwner(owner: String, conversationId: String) =
        messageDao.getLastMessageNotSendByOwner(owner, conversationId)

    fun getMessageFlowById(messageId: Int) = messageDao.getMessageFlowById(messageId)

    suspend fun getMessageById(messageId: Int) = messageDao.getMessageById(messageId)

    suspend fun saveMessageContentUri(messageId: Int, contentUri: String) =
        messageDao.saveContentUri(messageId, contentUri)

}