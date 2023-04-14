package com.hanialjti.allchat.data.remote.xmpp

import com.hanialjti.allchat.data.local.datastore.xmpp.RosterItem
import com.hanialjti.allchat.data.remote.model.RoomState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface XmppClientDataSource {
    val nicknameStream: Flow<String?>
    val shouldSyncChatsStream: Flow<Boolean>
    val chatRooms: Flow<Set<RoomState>>
    val isAuthenticated: Flow<Boolean>
    val rosterItems: Flow<Set<RosterItem>>
    val rosterVersion: Flow<String>
    suspend fun updateNickname(nickname: String)
    suspend fun updateChatRoomState(newState: RoomState)
    suspend fun updateIsAuthenticated(isAuthenticated: Boolean)
    suspend fun addChatRooms(vararg roomAddress: RoomState)
    suspend fun removeChatRooms(vararg roomStates: RoomState)
    suspend fun removeChatRooms(vararg roomAddresses: String)
    suspend fun removeAllChatRooms()
    suspend fun updateRosterVersion(rosterVersion: String)
    suspend fun removeRosterItem(jid: String)
    suspend fun addRosterItem(rosterItem: RosterItem)
    suspend fun resetRosterItems(rosterItems: List<RosterItem>)
}