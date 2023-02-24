package com.hanialjti.allchat.data.remote

import com.hanialjti.allchat.data.local.room.entity.MessageEntity
import com.hanialjti.allchat.data.model.Marker
import com.hanialjti.allchat.data.remote.model.CallResult
import com.hanialjti.allchat.data.remote.model.MessagePage
import com.hanialjti.allchat.data.remote.model.RemoteMessage
import com.hanialjti.allchat.data.remote.model.RemoteMessageItem
import kotlinx.coroutines.flow.Flow

interface MessageRemoteDataSource {

    fun messageChangesStream(): Flow<RemoteMessageItem>
    suspend fun updateMarkerForMessage(message: RemoteMessage, marker: Marker): CallResult<String>

    /**
     * Sends a message to a 1:1 or group chat
     *
     * @param message the messages to be sent
     * @param isMarkable if the message is a markable one. meaning, should other user send a read or delivered marker
     * @return A CallResult object with the sent message id
     */
    suspend fun sendMessage(message: MessageEntity, thread: String?, isMarkable: Boolean): CallResult<String>
    suspend fun getPreviousPage(
        chatId: String,
        oldestMessage: RemoteMessage?,
        pageSize: Int
    ): MessagePage
    suspend fun getNextPage(
        chatId: String,
        newestMessage: RemoteMessage?,
        pageSize: Int
    ): MessagePage
    suspend fun syncMessages(
        lastMessage: RemoteMessage?,
        pageSize: Int
    ): MessagePage
}