package com.hanialjti.allchat.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.hanialjti.allchat.localdatabase.AllChatLocalRoomDatabase
import com.hanialjti.allchat.models.ChatState
import com.hanialjti.allchat.models.MessageOperation
import com.hanialjti.allchat.models.MessageQueryResult
import com.hanialjti.allchat.models.entity.Message
import com.hanialjti.allchat.models.entity.Status
import com.hanialjti.allchat.models.entity.StatusMessage
import com.hanialjti.allchat.paging.MessageRemoteMediator
import com.hanialjti.allchat.xmpp.XmppConnectionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class XmppChatRepository constructor(
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

    suspend fun markMessageAsDisplayed(messageId: String) = withContext(Dispatchers.IO) {
        val message = getMessageById(messageId)
        val sendResult = remoteDb.markMessageAsDisplayed(message)
        handleMessageOperation(sendResult)
    }

    suspend fun beginSendMessageWork(message: Message) {
        messageDao.upsertMessage(message)
    }

    suspend fun sendMessage(message: Message) = withContext(Dispatchers.IO) {
        return@withContext remoteDb
            .createAndSendMessage(message)
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

    suspend fun listenForMessageChanges() {
        remoteDb.listenForMessageChanges()
            .collect { message ->
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

    private suspend fun upsertMessage(message: MessageOperation.Created) {
        messageDao.upsertMessage(message.message)
        message.message.conversation?.let {
            conversationsDao.updateLastMessage(
                lastMessage = message.message.body,
                lastUpdated = message.message.timestamp,
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
                upsertMessage(messageOperation)
            }
            is MessageOperation.StatusChanged -> {
                upsertStatusChange(messageOperation.message)
                if (messageOperation.message.status == Status.Seen && messageOperation.message.owner != messageOperation.message.from) {
                    val message = messageDao.getMessageById(messageOperation.id)
                    updatePreviousMessagesStatus(message)
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

    fun getMessageFlowById(messageId: String) = messageDao.getMessageFlowById(messageId)

    suspend fun getMessageById(messageId: String) = messageDao.getMessageById(messageId)

    suspend fun saveMessageContentUri(messageId: String, contentUri: String) =
        messageDao.saveContentUri(messageId, contentUri)

}