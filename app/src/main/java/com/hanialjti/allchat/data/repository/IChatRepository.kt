package com.hanialjti.allchat.data.repository

import androidx.paging.PagingData
import com.hanialjti.allchat.data.local.room.entity.MessageEntity
import com.hanialjti.allchat.data.model.ChatState
import com.hanialjti.allchat.data.model.MessageItem
import com.hanialjti.allchat.data.remote.model.CallResult
import com.hanialjti.allchat.data.remote.model.MessageQueryResult
import kotlinx.coroutines.flow.Flow

interface IChatRepository {
    suspend fun setMessagesAsRead(chatId: String)
    suspend fun sendMessageAndRegisterForAcknowledgment(messageId: Int): CallResult<String>
    suspend fun retrievePreviousPage(
        chatId: String,
        oldestMessage: MessageEntity?,
        pageSize: Int
    ): MessageQueryResult
    suspend fun retrieveNextPage(
        chatId: String,
        newestMessage: MessageEntity?,
        pageSize: Int
    ): MessageQueryResult

    suspend fun syncMessages(owner: String)
    suspend fun getMessageByExternalId(externalMessageId: String): MessageEntity?
    fun observeLastMessageNotSentByOwner(owner: String, conversationId: String): Flow<MessageEntity>
    suspend fun sendMessage(message: MessageEntity)
    fun fetchMessagesFor(chatId: String): Flow<PagingData<MessageItem.MessageData>>
    suspend fun listenForMessageUpdates()
    suspend fun updateMyChatState(chatState: ChatState)
    suspend fun sendSeenMarkerForMessage(externalMessageId: String): CallResult<Boolean>
}