package com.hanialjti.allchat.data.remote

import com.hanialjti.allchat.common.model.ListChange
import com.hanialjti.allchat.data.remote.model.CallResult
import com.hanialjti.allchat.data.remote.model.RemoteChat
import com.hanialjti.allchat.data.remote.model.RemoteUserItem
import kotlinx.coroutines.flow.Flow

interface ChatRemoteDataSource {
    suspend fun createChatRoom(
        roomName: String,
        myId: String
    ): CallResult<String>
    suspend fun inviteUserToChatRoom(userId: String, conversationId: String, myId: String): CallResult<String>
    suspend fun retrieveContacts(): List<RemoteChat>
    // TODO: Start the conversation via a worker
    suspend fun addUserToContact(userId: String, userName: String): CallResult<String>
    fun listenForChatChanges(): Flow<RemoteChat>
}