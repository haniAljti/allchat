package com.hanialjti.allchat.data.remote.xmpp

import android.content.Context
import androidx.datastore.dataStore
import com.hanialjti.allchat.data.local.datastore.xmpp.RosterItem
import com.hanialjti.allchat.data.local.datastore.xmpp.XmppConfigsSerializer
import com.hanialjti.allchat.data.remote.model.RoomState
import kotlinx.coroutines.flow.*

class XmppClientDataStore(
    private val context: Context
) : XmppClientDataSource {

    private val Context.dataStore by dataStore(
        fileName = "xmpp_configs.json",
        serializer = XmppConfigsSerializer()
    )

    private val xmppConfigs = context.dataStore.data.distinctUntilChanged()

    override val nicknameStream: Flow<String?> = context.dataStore.data.map { it.nickname }
    override val shouldSyncChatsStream: Flow<Boolean> = xmppConfigs.map { it.chatRooms.isEmpty() }
    override val chatRooms: Flow<Set<RoomState>> = xmppConfigs.map { it.chatRooms }
    override val rosterItems: Flow<Set<RosterItem>> = xmppConfigs.map { it.rosterItems }
    override val rosterVersion: Flow<String> = xmppConfigs.map { it.rosterVersion }

    private val _isAuthenticated: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isAuthenticated: Flow<Boolean>
        get() = _isAuthenticated.asStateFlow()


    override suspend fun updateNickname(nickname: String) {
        context.dataStore.updateData {
            it.copy(nickname = nickname)
        }
    }

    override suspend fun updateChatRoomState(newState: RoomState) {
        context.dataStore.updateData { configs ->
            configs.copy(
                chatRooms = configs.chatRooms.toMutableSet()
                    .apply { removeIf { newState.id == it.id }; add(newState) })
        }
    }

    override suspend fun resetRosterItems(rosterItems: List<RosterItem>) {
        context.dataStore.updateData { configs ->
            configs.copy(
                rosterItems = rosterItems.toMutableSet()
            )
        }
    }

    override suspend fun addRosterItem(rosterItem: RosterItem) {
        context.dataStore.updateData { configs ->
            configs.copy(
                rosterItems = configs.rosterItems.toMutableSet()
                    .apply { add(rosterItem) })
        }
    }

    override suspend fun removeRosterItem(jid: String) {
        context.dataStore.updateData { configs ->
            configs.copy(
                rosterItems = configs.rosterItems.toMutableSet()
                    .apply { removeIf { jid == it.jid } })
        }
    }

    override suspend fun updateRosterVersion(rosterVersion: String) {
        context.dataStore.updateData { it.copy(rosterVersion = rosterVersion) }
    }

    override suspend fun updateIsAuthenticated(isAuthenticated: Boolean) {
        _isAuthenticated.update { isAuthenticated }
    }

    override suspend fun addChatRooms(vararg roomAddress: RoomState) {
        context.dataStore.updateData { configs ->
            configs.copy(
                chatRooms = configs.chatRooms.toMutableSet()
                    .apply { roomAddress.forEach { if (!contains(it)) add(it) } })
        }
    }

    override suspend fun removeChatRooms(vararg roomStates: RoomState) {
        context.dataStore.updateData { configs ->
            configs.copy(
                chatRooms = configs.chatRooms.toMutableSet()
                    .apply { roomStates.forEach { if (contains(it)) remove(it) } })
        }
    }

    override suspend fun removeChatRooms(vararg roomAddresses: String) {
        context.dataStore.updateData { configs ->
            configs.copy(
                chatRooms = configs.chatRooms.toMutableSet()
                    .apply {
                        roomAddresses.forEach { roomAddress ->
                            if (any { it.id == roomAddress })
                                removeIf { it.id == roomAddress }
                        }
                    })
        }
    }

    override suspend fun removeAllChatRooms() {
        context.dataStore.updateData { configs ->
            configs.copy(
                chatRooms = setOf()
            )
        }
    }
}