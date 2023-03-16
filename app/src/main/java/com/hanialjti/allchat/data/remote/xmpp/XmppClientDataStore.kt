package com.hanialjti.allchat.data.remote.xmpp

import android.content.Context
import androidx.datastore.dataStore
import com.hanialjti.allchat.data.local.datastore.xmpp.XmppConfigsSerializer
import kotlinx.coroutines.flow.*

class XmppClientDataStore(
    private val context: Context,
) : XmppClientDataSource {

    private val Context.dataStore by dataStore(
        fileName = "xmpp_configs.json",
        serializer = XmppConfigsSerializer()
    )

    private val xmppConfigs = context.dataStore.data.distinctUntilChanged()

    override val nicknameStream: Flow<String?> = context.dataStore.data.map { it.nickname }
    override val shouldSyncChatsStream: Flow<Boolean> = xmppConfigs.map { it.chatRooms.isEmpty() }
    override val chatRooms: Flow<Set<String>> = xmppConfigs.map { it.chatRooms }

    private val _isAuthenticated: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isAuthenticated: Flow<Boolean>
        get() = _isAuthenticated.asStateFlow()



    override suspend fun updateNickname(nickname: String) {
        context.dataStore.updateData {
            it.copy(nickname = nickname)
        }
    }

    override suspend fun updateIsAuthenticated(isAuthenticated: Boolean) {
        _isAuthenticated.update { isAuthenticated }
    }

    override suspend fun addChatRooms(vararg roomAddress: String) {
        context.dataStore.updateData { configs ->
            configs.copy(
                chatRooms = configs.chatRooms.toMutableSet()
                    .apply { roomAddress.forEach { if (!contains(it)) add(it) } })
        }
    }

    override suspend fun removeChatRooms(vararg roomAddress: String) {
        context.dataStore.updateData { configs ->
            configs.copy(
                chatRooms = configs.chatRooms.toMutableSet()
                    .apply { roomAddress.forEach { if (contains(it)) remove(it) } })
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