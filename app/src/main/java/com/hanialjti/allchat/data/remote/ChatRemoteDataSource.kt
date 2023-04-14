package com.hanialjti.allchat.data.remote

import com.hanialjti.allchat.data.model.ChatState
import com.hanialjti.allchat.data.model.Participant
import com.hanialjti.allchat.data.remote.model.CallResult
import com.hanialjti.allchat.data.remote.model.ChatUpdate
import com.hanialjti.allchat.data.remote.model.RemoteRoomInfo
import kotlinx.coroutines.flow.Flow

interface ChatRemoteDataSource {

    suspend fun updateMyChatState(chatState: ChatState)
    fun chatUpdatesStream(): Flow<ChatUpdate>
    suspend fun createChatRoom(
        roomName: String,
        invitees: Set<String>
    ): CallResult<String>
    suspend fun inviteUserToChatRoom(userId: String, conversationId: String, myId: String): CallResult<String>
    suspend fun addUserToContact(userId: String): CallResult<String>

    suspend fun updateChatInfo(
        chatId: String,
        description: String?,
        avatarUrl: String?,
        subject: String?
    ): CallResult<Boolean>

    suspend fun updateMyChatState(chatId: String, state: Participant.State)
    suspend fun getRoomInfo(roomId: String): CallResult<RemoteRoomInfo>
    suspend fun getRoomSubject(roomId: String): CallResult<String>
}