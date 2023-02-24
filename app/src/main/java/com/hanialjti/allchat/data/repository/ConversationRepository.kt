package com.hanialjti.allchat.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.room.withTransaction
import com.hanialjti.allchat.R
import com.hanialjti.allchat.data.local.room.AllChatLocalRoomDatabase
import com.hanialjti.allchat.data.local.room.entity.*
import com.hanialjti.allchat.data.local.room.model.toContact
import com.hanialjti.allchat.data.model.ChatState
import com.hanialjti.allchat.data.model.Contact
import com.hanialjti.allchat.data.model.MessageSummary
import com.hanialjti.allchat.data.remote.ChatRemoteDataSource
import com.hanialjti.allchat.data.remote.ConnectionManager
import com.hanialjti.allchat.data.remote.model.CallResult
import com.hanialjti.allchat.data.remote.model.ChatStateUpdate
import com.hanialjti.allchat.data.remote.model.NewContactUpdate
import com.hanialjti.allchat.data.tasks.ConversationTasksDataStore
import com.hanialjti.allchat.presentation.conversation.ContactContent
import com.hanialjti.allchat.presentation.conversation.UiText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import timber.log.Timber

class ConversationRepository(
    private val localDataStore: AllChatLocalRoomDatabase,
    private val remoteDataStore: ChatRemoteDataSource,
    private val tasksDataStore: ConversationTasksDataStore,
    private val connectionManager: ConnectionManager,
    private val userRepository: UserRepository,
    private val infoRepository: InfoRepository
) {

    private val conversationDao = localDataStore.conversationDao()
    private val participantDao = localDataStore.participantDao()

    private val ownerId get() = connectionManager.userId

    private fun contacts(owner: String) = Pager(
        config = PagingConfig(pageSize = 30),
        pagingSourceFactory = { conversationDao.getContactsWithLastMessage(owner) }
    ).flow.map {
        it.map { contactEntry ->
            val contact = contactEntry.toContact()
            contact.copy(
                    content = getConversationContent(
                        contact,
                        contact.lastMessage
                    )
                )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun myContacts(): Flow<PagingData<Contact>> = userRepository.loggedInUser.flatMapLatest {
        it?.let { contacts(it) } ?: emptyFlow()
    }

    private suspend fun getConversationContent(
        contact: Contact,
        lastMessage: MessageSummary?
    ): ContactContent? {

        return if (contact.isGroupChat && contact.composing.isNotEmpty()) {
            val users = userRepository.getUsers(contact.composing)
            ContactContent.Composing(
                UiText.PluralStringResource(
                    R.plurals.composing,
                    contact.composing.size,
                    users.joinToString()
                )
            )
        } else if (!contact.isGroupChat && contact.composing.isNotEmpty()) {
            ContactContent.Composing(UiText.StringResource(R.string.composing))
        } else {
            lastMessage.let { message ->
                message?.body?.let {
                    ContactContent.LastMessage(
                        text = UiText.DynamicString(message.body),
                        read = contact.unreadMessages == 0
                    )
                }
            }
        }
    }

    suspend fun updateMyChatState(chatState: ChatState) {
        remoteDataStore.updateMyChatState(chatState)
    }

    fun contact(userId: String): Flow<Contact?> {
        return conversationDao.getChatWithLastMessage(userId).map { it?.toContact() }
    }

    suspend fun resetUnreadCounter(conversationId: String) {
        conversationDao.resetUnreadCounter(conversationId)
    }

//    suspend fun getRoomInfo(roomId: String) = remoteDataStore.getRoomInfo(roomId)

    suspend fun createChatRoom(roomExternalId: String): CallResult<String> {

        val username = ownerId ?: return CallResult.Error("user is not logged in!")

        val conversationEntity = contact(roomExternalId)

        return conversationEntity.first()?.name?.let { nickName ->
            remoteDataStore.createChatRoom(nickName, username)
        } ?: CallResult.Error("Room must have a name")
    }

    suspend fun inviteUserToChatRoom(userId: String, conversationId: String, myId: String) {
        remoteDataStore.inviteUserToChatRoom(userId, conversationId, myId)
    }

    suspend fun insert(conversation: ChatEntity) {
        conversationDao.insertOrIgnore(conversation)
    }

    suspend fun syncChats() {
        ownerId?.let { owner ->
            val chats = remoteDataStore.retrieveContacts()
            chats.forEach { remoteChat ->
                localDataStore.withTransaction {

                    if (remoteChat.isGroupChat) {
                        val chatEntity = ChatEntity(
                            id = remoteChat.id,
                            owner = owner,
                            isGroupChat = true,
                            name = remoteChat.name,
                        )

                        insert(chatEntity)
                    } else {
                        createUserAndAddToContactList(remoteChat.id, owner)
                    }
                }
            }
        } ?: Timber.e("User is not logged in!")
    }

    suspend fun addUserToContactList(userId: String): CallResult<String> {
        val owner = ownerId ?: return CallResult.Error("User is not signed in")

        val chat = createUserAndAddToContactList(userId, owner) ?: return CallResult.Error(
            "An error occurred while trying to add user to contact list"
        )

        return remoteDataStore.addUserToContact(userId, chat.name ?: "AllChat User")
    }

    private suspend fun createUserAndAddToContactList(userId: String, owner: String): ChatEntity? =
        localDataStore.withTransaction {

            var chat = conversationDao.getConversationByRemoteId(userId)

            if (chat != null) {
                return@withTransaction chat
            }

            chat = ChatEntity(
                id = userId,
                isGroupChat = false,
                owner = owner
            )

            infoRepository.fetchAndSaveEntityInfo(userId)
            userRepository.getAndSaveUser(userId)
            conversationDao.insert(chat)

            participantDao.insertParticipants(
                ParticipantEntity(
                    userId = userId,
                    chatId = userId,
                )
            )

            return@withTransaction chat
        }

    private suspend fun upsertChatRoom(
        roomAddress: String,
        name: String?,
        owner: String
    ): ChatEntity = localDataStore.withTransaction {

        var chat = conversationDao.getConversationByRemoteId(roomAddress)
        if (chat != null) {
            return@withTransaction chat
        }

        chat = ChatEntity(
            id = roomAddress,
            isGroupChat = false,
            name = name,
            avatar = null,
            owner = owner
        )

        conversationDao.insert(chat)
        return@withTransaction chat
    }

    suspend fun listenForConversationUpdates() = remoteDataStore
        .listenForChatUpdates()
        .combine(userRepository.loggedInUser) { rosterChange, loggedInUser ->
            loggedInUser ?: return@combine

            rosterChange.forEach { rosterChange ->
                when (rosterChange) {
                    is NewContactUpdate -> {
                        if (!rosterChange.isGroupChat) {
                            Timber.d("New 1:1 chat id ${rosterChange.chatId}")
                            createUserAndAddToContactList(
                                rosterChange.chatId,
                                loggedInUser
                            )
                        } else {
                            Timber.d("New Group chat id ${rosterChange.chatId}")
                            upsertChatRoom(
                                rosterChange.chatId,
                                null,
                                loggedInUser
                            )
                        }
                    }
                    is ChatStateUpdate -> {
                        //TODO
                    }
                }
            }

        }

}