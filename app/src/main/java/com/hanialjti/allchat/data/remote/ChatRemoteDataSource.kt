package com.hanialjti.allchat.data.remote

import com.hanialjti.allchat.common.model.ListChange
import com.hanialjti.allchat.data.model.ChatState
import com.hanialjti.allchat.data.remote.model.CallResult
import com.hanialjti.allchat.data.remote.model.ChatUpdate
import com.hanialjti.allchat.data.remote.model.RemoteChat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface ChatRemoteDataSource {

    suspend fun updateMyChatState(chatState: ChatState)

    fun chatUpdatesStream(): Flow<ChatUpdate>

    suspend fun createChatRoom(
        roomName: String,
        myId: String,
        invitees: Set<String>
    ): CallResult<String>
    suspend fun inviteUserToChatRoom(userId: String, conversationId: String, myId: String): CallResult<String>
    suspend fun retrieveGroupChats(): List<String>
    // TODO: Start the conversation via a worker
    suspend fun addUserToContact(userId: String): CallResult<String>
//    suspend fun emitChatChange(change: ListChange<RemoteChat>)
//    fun startListeners()
//    fun stopListeners()
}