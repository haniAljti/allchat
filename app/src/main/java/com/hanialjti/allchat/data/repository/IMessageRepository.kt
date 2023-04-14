package com.hanialjti.allchat.data.repository

import androidx.paging.PagingData
import com.hanialjti.allchat.data.local.room.entity.MessageEntity
import com.hanialjti.allchat.data.model.Attachment
import com.hanialjti.allchat.data.model.ChatState
import com.hanialjti.allchat.data.model.MessageItem
import com.hanialjti.allchat.data.remote.model.CallResult
import com.hanialjti.allchat.data.remote.model.MessageQueryResult
import com.hanialjti.allchat.data.remote.model.RemoteMessageItem
import kotlinx.coroutines.flow.Flow

interface IMessageRepository {
    suspend fun setMessagesAsRead(chatId: String)
    suspend fun sendMessageAndRegisterForAcknowledgment(messageId: String): CallResult<String>
    suspend fun retrievePreviousPage(
        chatId: String,
        pageSize: Int
    ): MessageQueryResult
    suspend fun retrieveNextPage(
        chatId: String,
        pageSize: Int
    ): MessageQueryResult

    suspend fun getAllPendingMessages(): List<MessageEntity>
    suspend fun downloadAttachment(message: MessageItem.MessageData)
    suspend fun resendAllPendingMessages()
    suspend fun syncMessages(chatId: String)
    suspend fun getMessageByExternalId(externalMessageId: String): Flow<MessageItem.MessageData>?
    suspend fun getMessage(messageId: String): MessageEntity?
    fun observeLastMessageNotSentByOwner(owner: String, conversationId: String): Flow<MessageEntity>
    fun sendMessage(
        body: String?,
        replyingTo: String?,
        contactId: String,
        isGroupChat: Boolean,
        attachment: Attachment?,
    )
    fun fetchMessagesFor(chatId: String): Flow<PagingData<MessageItem>>

    fun messageUpdatesStream(): Flow<MessageItem.MessageData>
    suspend fun sendSeenMarkerForMessage(externalMessageId: String): CallResult<Boolean>
    suspend fun updateMessage(message: MessageEntity)

    suspend fun updateAttachment(messageId: String, attachment: Attachment)

}