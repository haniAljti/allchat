package com.hanialjti.allchat.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.hanialjti.allchat.R
import com.hanialjti.allchat.common.utils.Logger
import com.hanialjti.allchat.data.local.room.dao.ConversationDao
import com.hanialjti.allchat.data.local.room.entity.*
import com.hanialjti.allchat.data.local.room.model.toContact
import com.hanialjti.allchat.data.model.ChatState
import com.hanialjti.allchat.data.model.ContactWithLastMessage
import com.hanialjti.allchat.data.model.MessageSummary
import com.hanialjti.allchat.data.remote.ChatRemoteDataSource
import com.hanialjti.allchat.data.remote.model.CallResult
import com.hanialjti.allchat.data.remote.model.ChatStateUpdate
import com.hanialjti.allchat.data.remote.model.NewContactUpdate
import com.hanialjti.allchat.presentation.conversation.ContactContent
import com.hanialjti.allchat.presentation.conversation.UiText
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber

class ConversationRepository(
    private val conversationDao: ConversationDao,
    private val remoteDataStore: ChatRemoteDataSource,
    private val userRepository: UserRepository,
    private val authenticationRepository: AuthenticationRepository,
    private val infoRepository: InfoRepository,
    private val dispatcher: CoroutineDispatcher,
    private val externalScope: CoroutineScope
) {
    private suspend fun loggedInUser() = authenticationRepository.loggedInUserStream.first()

    private fun contacts(owner: String) = Pager(
        config = PagingConfig(pageSize = 30),
        pagingSourceFactory = { conversationDao.getContactsWithLastMessage(owner) }
    ).flow
        .map {
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
    fun myContacts(): Flow<PagingData<ContactWithLastMessage>> = authenticationRepository
        .loggedInUserStream
        .flatMapLatest {
            it?.let { contacts(it) } ?: emptyFlow()
        }

    private suspend fun getConversationContent(
        contactWithLastMessage: ContactWithLastMessage,
        lastMessage: MessageSummary?
    ): ContactContent? {

        return if (contactWithLastMessage.isGroupChat && contactWithLastMessage.composing.isNotEmpty()) {
            val users = userRepository.getUsers(contactWithLastMessage.composing)
            ContactContent.Composing(
                UiText.PluralStringResource(
                    R.plurals.composing,
                    contactWithLastMessage.composing.size,
                    users.joinToString()
                )
            )
        } else if (!contactWithLastMessage.isGroupChat && contactWithLastMessage.composing.isNotEmpty()) {
            ContactContent.Composing(UiText.StringResource(R.string.composing))
        } else {
            lastMessage.let { message ->
                message?.body?.let {
                    ContactContent.LastMessage(
                        text = UiText.DynamicString(message.body),
                        read = contactWithLastMessage.unreadMessages == 0
                    )
                }
            }
        }
    }

    suspend fun updateMyChatState(chatState: ChatState) {
        remoteDataStore.updateMyChatState(chatState)
    }

    fun contact(userId: String): Flow<ContactWithLastMessage?> {
        return conversationDao.getChatWithLastMessage(userId).map { it?.toContact() }
    }

    suspend fun resetUnreadCounter(conversationId: String) {
        conversationDao.resetUnreadCounter(conversationId)
    }

    suspend fun createChatRoom(
        name: String,
        image: String,
        usersToInvite: Set<String>
    ): CallResult<String> {

        val owner = loggedInUser() ?: return CallResult.Error("user is not logged in!")

        val result = remoteDataStore.createChatRoom(
            name,
            owner,
            usersToInvite
        )

        if (result is CallResult.Success) {
            result.data?.let {

                val chat = ChatEntity(
                    id = it,
                    isGroupChat = true,
                    owner = owner,
                )

                conversationDao.insert(chat)
                infoRepository.fetchAndSaveInfo(it, true)
            }
        }

        return CallResult.Success()
    }

    suspend fun inviteUserToChatRoom(userId: String, conversationId: String, myId: String) {
        remoteDataStore.inviteUserToChatRoom(userId, conversationId, myId)
    }

    suspend fun addUserToContactList(userId: String): CallResult<String> {
        val owner = loggedInUser() ?: return CallResult.Error("User is not signed in")

        createUserAndAddToContactList(userId, owner)

        return remoteDataStore.addUserToContact(userId)
    }

    private suspend fun createUserAndAddToContactList(userId: String, owner: String): ChatEntity =
        withContext(dispatcher) {

            var chat = conversationDao.getConversationByRemoteId(userId)

            if (chat != null) return@withContext chat

            chat = ChatEntity(
                id = userId,
                isGroupChat = false,
                owner = owner
            )

            conversationDao.insert(chat)
            infoRepository.fetchAndSaveInfo(userId, false)
            userRepository.getAndSaveUser(userId)

            return@withContext chat
        }

    private suspend fun upsertChatRoom(
        roomAddress: String,
        owner: String
    ): ChatEntity = withContext(dispatcher) {

        var chat = conversationDao.getConversationByRemoteId(roomAddress)
        if (chat != null) {
            return@withContext chat
        }

        chat = ChatEntity(
            id = roomAddress,
            isGroupChat = true,
            owner = owner
        )

        conversationDao.insert(chat)
        infoRepository.fetchAndSaveInfo(roomAddress, true)
        return@withContext chat
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun listenForConversationUpdates() = authenticationRepository
        .loggedInUserStream
        .flatMapLatest { owner ->
            Logger.d { "owner is $owner" }
            if (owner == null) flowOf()
            else {
                remoteDataStore
                    .chatUpdatesStream()
                    .map { chatUpdate ->
                        externalScope.async {
                            withContext(dispatcher) {
                                Logger.d { "New chat update" }
                                return@withContext when (chatUpdate) {
                                    is NewContactUpdate -> {
                                        if (!chatUpdate.isGroupChat) {
                                            Timber.d("New 1:1 chat id ${chatUpdate.chatId}")
                                            createUserAndAddToContactList(
                                                chatUpdate.chatId,
                                                owner
                                            )
                                        } else {
                                            Timber.d("New Group chat id ${chatUpdate.chatId}")
                                            upsertChatRoom(
                                                chatUpdate.chatId,
                                                owner
                                            )
                                        }
                                        chatUpdate
                                    }
                                    is ChatStateUpdate -> {
                                        //TODO
                                        null
                                    }
                                }
                            }
                        }.await()
                    }
            }
        }
        .filter { it is NewContactUpdate }
        .filterNotNull()
        .map { it.chatId }


}

