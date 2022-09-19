package com.hanialjti.allchat.repository

import androidx.room.withTransaction
import com.hanialjti.allchat.localdatabase.AllChatLocalRoomDatabase
import com.hanialjti.allchat.models.ConversationAndUser
import com.hanialjti.allchat.models.ListChange
import com.hanialjti.allchat.models.entity.Conversation
import com.hanialjti.allchat.models.entity.User
import com.hanialjti.allchat.viewmodels.defaultName
import com.hanialjti.allchat.xmpp.XmppConnectionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class ConversationRepository constructor(
    private val localDb: AllChatLocalRoomDatabase,
    private val remoteDb: XmppConnectionHelper
) {

    private val conversationDao = localDb.conversationDao()
    private val userDao = localDb.userDao()

    fun conversations(owner: String) =
        conversationDao.getConversationsAndUsers(owner)

    fun conversation(conversationId: String) =
        conversationDao.getConversationAndUser(conversationId)

    suspend fun resetUnreadCounter(conversationId: String) {
        conversationDao.resetUnreadCounter(conversationId)
    }

    suspend fun insertConversationAndUser(conversationAndUser: ConversationAndUser) =
        withContext(Dispatchers.IO) {
            localDb.withTransaction {
                conversationDao.upsert(conversationAndUser.conversation)
                conversationAndUser.user?.let { user -> userDao.upsert(user) }
            }
            conversationAndUser.conversation.from?.let {
                startConversation(
                    conversationAndUser.conversation.id,
                    conversationAndUser.name,
                    conversationAndUser.conversation.isGroupChat,
                    it
                )
            }
        }

    suspend fun loadAllContacts(owner: String) {
        remoteDb.retrieveContacts(owner)
            .forEach {
                insertConversationAndUser(it)
            }
    }

    private suspend fun startConversation(
        conversationId: String,
        name: String?,
        isGroupChat: Boolean,
        myId: String
    ) = withContext(Dispatchers.IO) {

        val conversation = Conversation(
            id = conversationId,
            isGroupChat = isGroupChat,
            name = name,
            from = myId,
            to = if (!isGroupChat) conversationId else null
        )

        conversationDao.insert(conversation)

        // TODO: Do this in a worker
        remoteDb.startConversation(
            conversationId,
            name ?: defaultName,
            myId
        )
    }

    suspend fun listenForConversationUpdates() {
        remoteDb.listenForRosterChanges()
            .collect { rosterChange ->
                Timber.d("New Change to the roster")
                when (rosterChange) {
                    is ListChange.ItemAdded -> {
                        if (rosterChange.item is Conversation) {
                            Timber.d("New Contact")
                            val conversation = rosterChange.item
                            conversationDao.insert(conversation)
                        }
                    }
                    is ListChange.ItemUpdated -> {
                        if (rosterChange.item is User) {
                            Timber.d("User data change")
                            val user = rosterChange.item
                            userDao.updateUserPresence(user.isOnline)
                        }
                    }
                }
            }
    }
}