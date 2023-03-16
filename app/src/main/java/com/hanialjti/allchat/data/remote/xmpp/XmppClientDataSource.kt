package com.hanialjti.allchat.data.remote.xmpp

import kotlinx.coroutines.flow.Flow

interface XmppClientDataSource {
    val nicknameStream: Flow<String?>
    val shouldSyncChatsStream: Flow<Boolean>
    val chatRooms: Flow<Set<String>>
    val isAuthenticated: Flow<Boolean>

    suspend fun updateNickname(nickname: String)
    suspend fun updateIsAuthenticated(isAuthenticated: Boolean)
    suspend fun addChatRooms(vararg roomAddress: String)
    suspend fun removeChatRooms(vararg roomAddress: String)
    suspend fun removeAllChatRooms()
}