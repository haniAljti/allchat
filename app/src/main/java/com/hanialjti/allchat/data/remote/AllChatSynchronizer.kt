package com.hanialjti.allchat.data.remote

import com.hanialjti.allchat.common.utils.Logger
import com.hanialjti.allchat.data.local.datastore.PreferencesLocalDataStore
import com.hanialjti.allchat.data.remote.xmpp.XmppClientDataSource
import com.hanialjti.allchat.data.repository.ConversationRepository
import com.hanialjti.allchat.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

class AllChatSynchronizer(
    private val preferencesLocalDataStore: PreferencesLocalDataStore,
    private val messageRepository: MessageRemoteDataSource,
    private val clientDataStore: XmppClientDataSource,
    private val conversationRepository: ConversationRepository,
    private val userRepository: UserRepository
) {

    private val isSynced = AtomicBoolean()

    suspend fun synchronize(): Boolean =
        withContext(Dispatchers.IO) {
            try {
                if (!isSynced.get()) {
//                    preferencesLocalDataStore.updateIsSynced(false)
//                    Logger.d { "Syncing with server" }
//                    if (userRepository.getMyUserDetails() == null) {
//                        userRepository.getAndSaveUser()
//                    }
//                    val owner = preferencesLocalDataStore.usernameStream.first()
//                        ?: return@withContext false // No user is logged in
//                    val chats = conversationRepository.getAllRemoteChats()
//                    Logger.d { "Synchronizer fetched ${chats.size} items" }
//                    Logger.d { "$chats" }
//                    val chatJob = chats.map {
//                        async {
//                            conversationRepository.insertChat(it.id, owner, it.isGroupChat)
//                            if (it.isGroupChat) {
//                                clientDataStore.addChatRooms(RoomState(it.id))
//                            }
//                        }
//                    }
//                    chatJob.awaitAll()
                }
                preferencesLocalDataStore.updateIsSynced(true)
                isSynced.set(true)
                true
            } catch (e: Exception) {
                Logger.e(e)
                false
            }
        }
}
