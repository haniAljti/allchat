package com.hanialjti.allchat.data.repository

import androidx.paging.*
import com.hanialjti.allchat.R
import com.hanialjti.allchat.common.utils.Logger
import com.hanialjti.allchat.data.local.room.AllChatLocalRoomDatabase
import com.hanialjti.allchat.data.local.room.dao.ConversationDao
import com.hanialjti.allchat.data.local.room.dao.InfoDao
import com.hanialjti.allchat.data.local.room.dao.ParticipantDao
import com.hanialjti.allchat.data.local.room.entity.*
import com.hanialjti.allchat.data.local.room.model.asChatDetails
import com.hanialjti.allchat.data.local.room.model.toContact
import com.hanialjti.allchat.data.model.*
import com.hanialjti.allchat.data.remote.ChatRemoteDataSource
import com.hanialjti.allchat.data.remote.model.*
import com.hanialjti.allchat.presentation.conversation.ContactContent
import com.hanialjti.allchat.presentation.conversation.ContactImage
import com.hanialjti.allchat.presentation.conversation.UiText
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class ConversationRepository(
    private val chatLocalDataSource: AllChatLocalRoomDatabase,
    private val remoteDataStore: ChatRemoteDataSource,
    private val userRepository: UserRepository,
    private val authenticationRepository: AuthRepository,
    private val fileRepository: FileRepository,
    private val dispatcher: CoroutineDispatcher,
    private val externalScope: CoroutineScope,
    private val conversationDao: ConversationDao = chatLocalDataSource.conversationDao(),
    private val participantDao: ParticipantDao = chatLocalDataSource.participantDao(),
    private val infoDao: InfoDao = chatLocalDataSource.infoDao(),
) {

    private suspend fun loggedInUser() = authenticationRepository.loggedInUserStream.first()

    private fun contacts(owner: String) = Pager(config = PagingConfig(pageSize = 30),
        pagingSourceFactory = { conversationDao.getContactsWithLastMessage(owner) }).flow.map {
        it.map { contactEntry ->
            val contact = contactEntry.toContact()
            contact.copy(
                content = getConversationContent(
                    contact, contact.lastMessage
                )
            ).let { if (it.isGroupChat) it.copy(isOnline = false) else it }
        }.filter { !it.name.isNullOrEmpty() }
    }


    suspend fun updateRoomInfo(
        chatId: String, desc: String?, avatar: ContactImage?, subject: String?
    ): CallResult<Boolean> =
        withContext(externalScope.coroutineContext + dispatcher) {
            val avatarUrl: String? = when (avatar) {
                is ContactImage.DynamicImage -> {
                    infoDao.getOne(chatId)?.avatarUrl
                }
                is ContactImage.DynamicRawImage -> {
                    val avatarFile = fileRepository.createNewAvatarFile(chatId)
                    fileRepository.downloadAndSaveToInternalStorage(avatar.bytes, avatarFile)
                    val urlResult = fileRepository.uploadFile(avatarFile)
                    if (urlResult is CallResult.Success) {
                        urlResult.data
                    } else null
                }
                else -> null
            }

            remoteDataStore.updateChatInfo(chatId, desc, avatarUrl, subject)
        }


    @OptIn(ExperimentalCoroutinesApi::class)
    fun myContacts(): Flow<PagingData<ContactWithLastMessage>> =
        authenticationRepository.loggedInUserStream.flatMapLatest {
            it?.let { contacts(it) } ?: emptyFlow()
        }

    private suspend fun getConversationContent(
        contactWithLastMessage: ContactWithLastMessage, lastMessage: MessageSummary?
    ): ContactContent? {

        return if (contactWithLastMessage.isGroupChat && contactWithLastMessage.composing.isNotEmpty()) {
            val users = userRepository.getUsers(contactWithLastMessage.composing)
            ContactContent.Composing(
                UiText.PluralStringResource(
                    R.plurals.composing, contactWithLastMessage.composing.size, users.joinToString()
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

    fun updateMyChatState(chatId: String, state: Participant.State) {
        externalScope.launch(dispatcher) {
            remoteDataStore.updateMyChatState(chatId, state)
        }
    }

    suspend fun getChatDetails(userId: String): ChatDetails? {
        val owner = loggedInUser() ?: return null
        val chatInfo = conversationDao.getChatInfo(owner, userId).first() ?: return null
        val participantsInfo = infoDao.getParticipantsInfo(userId).first()
        return chatInfo.asChatDetails().copy(participants = participantsInfo.map {
            ParticipantInfo(id = it.userId,
                nickname = it.nickname,
                avatar = it.cachePath?.let { ContactImage.DynamicImage(it) }
                    ?: ContactImage.DefaultProfileImage(false),
                state = it.state,
                role = it.role)
        }.sortedByDescending { it.role.value }.toSet())

    }

    suspend fun getChatDetailsStream(userId: String): Flow<ChatDetails?> {
        val owner = loggedInUser() ?: return flowOf()
        val chatInfoStream = conversationDao.getChatInfo(owner, userId)
        val participantsInfoStream = infoDao.getParticipantsInfo(userId)
        return chatInfoStream.combine(participantsInfoStream) { chatInfo, participantsInfo ->

            val participants = participantsInfo.map {
                ParticipantInfo(id = it.userId,
                    nickname = it.nickname,
                    avatar = it.cachePath?.let { ContactImage.DynamicImage(it) }
                        ?: ContactImage.DefaultProfileImage(false),
                    state = it.state,
                    role = it.role)
            }
            chatInfo?.asChatDetails()
                ?.copy(
                    createdBy = participants.firstOrNull { it.role == Role.Owner },
                    participants = participants.sortedByDescending { it.role.value }.toSet()
                )
        }
    }

    suspend fun resetUnreadCounter(conversationId: String) {
        conversationDao.resetUnreadCounter(conversationId)
    }

    suspend fun createChatRoom(
        name: String, image: String, usersToInvite: Set<String>
    ): CallResult<String> {

        val owner = loggedInUser() ?: return CallResult.Error("user is not logged in!")

        val result = remoteDataStore.createChatRoom(
            name, usersToInvite
        )

        if (result is CallResult.Success) {
            result.data?.let {
                conversationDao.insert(
                    ChatEntity(
                        id = it,
                        isGroupChat = true,
                        owner = owner,
                    )
                )
            }
        }

        return result
    }

    suspend fun inviteUserToChatRoom(userId: String, conversationId: String) {
        val owner = loggedInUser() ?: return
        externalScope.launch {
            withContext(dispatcher) {
                remoteDataStore.inviteUserToChatRoom(userId, conversationId, owner)
            }
        }
    }

    suspend fun addUserToContactList(userId: String): CallResult<String> {
        val owner = loggedInUser() ?: return CallResult.Error("User is not signed in")

        insertChat(userId, owner, false)

        return remoteDataStore.addUserToContact(userId)
    }

    private suspend fun insertChat(
        chatId: String, owner: String, isGroupChat: Boolean
    ): ChatEntity {
        var chat = conversationDao.getConversationByRemoteId(chatId)
        if (isGroupChat) {

            if (chat != null) return chat

            chat = ChatEntity(
                id = chatId, isGroupChat = true, owner = owner
            )

            conversationDao.insert(chat)
//            infoRepository.fetchAndSaveInfo(chatId, true)
//            updateChatRoomInfo(chatId)
            return chat
        } else {

            if (chat != null) return chat

            chat = ChatEntity(
                id = chatId, isGroupChat = false, owner = owner
            )

            conversationDao.insert(chat)
//            infoRepository.fetchAndSaveInfo(chatId, false)
            userRepository.getAndSaveUser(chatId)

            return chat
        }
    }

//    private suspend fun updateChatRoomInfo(chatId: String) {
//        val roomInfoResult = remoteDataStore.getRoomInfo(chatId)
//
//        if (roomInfoResult is CallResult.Success) {
//
//            val roomInfo = roomInfoResult.data!!
//            roomInfo.description?.let {
//                conversationDao.updateDescription(chatId, it)
//            }
//
//            roomInfo.participants
//                .map {
//                    externalScope.async {
//                        userRepository.getAndSaveUser(it.id)
//                        participantDao.insertParticipants(
//                            ParticipantEntity(chatId = chatId, userId = it.id, role = it.role)
//                        )
//                    }
//                }.awaitAll()
//        }
//    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun listenForConversationUpdates() =
        authenticationRepository
            .loggedInUserStream
            .flatMapLatest { owner ->
                Logger.d { "owner is $owner" }
                if (owner == null) flowOf()
                else {
                    remoteDataStore
                        .chatUpdatesStream()
                        .mapNotNull { chatUpdate ->
//                        externalScope.async {
//                            withContext(dispatcher) {

                            when (chatUpdate) {
                                is OneOnOneChatAdded -> {
                                    Logger.d {
                                        "New 1:1 chat with id '${chatUpdate.chatId}'"
                                    }
                                    insertChat(
                                        chatUpdate.chatId,
                                        owner,
                                        false
                                    )
                                    chatUpdate
                                }
                                is ChatUpdated, is OneOnOneChatDeleted -> {
                                    chatUpdate
                                }
                                is MultiUserChatStateUpdate -> {
                                    Logger.d { "'${chatUpdate.chatId}'s state has changed. New State: $chatUpdate" }
                                    insertChat(
                                        chatUpdate.chatId, owner, true
                                    )
                                    conversationDao.updateDescription(
                                        chatUpdate.chatId,
                                        chatUpdate.roomState.description,
                                        chatUpdate.roomState.createdAt
                                    )
//                                        conversationDao
//                                            .getConversationByRemoteId(chatUpdate.chatId)
//                                            ?.copy(
//                                                description = chatUpdate.roomState.description,
//                                                createdAt = chatUpdate.roomState.createdAt
//                                            )
//                                            ?: ChatEntity(
//                                                id = chatUpdate.chatId,
//                                                description = chatUpdate.roomState.description,
//                                                owner = owner,
//                                                isGroupChat = true,
//                                                createdAt = chatUpdate.roomState.createdAt
//                                            ).also {
//                                                conversationDao.insert(it)
//                                            }
                                    val info = infoDao.getOne(chatUpdate.chatId)
                                    val oldAvatarUrl = info?.avatarUrl
                                    val newAvatarUrl = chatUpdate.roomState.avatarUrl
                                    val avatarCachePath =
                                        if (newAvatarUrl == oldAvatarUrl) info?.cachePath
                                        else newAvatarUrl?.let {
                                            cacheAvatarImage(it, chatUpdate.chatId)
                                        }
                                    infoDao.insert(
                                        InfoEntity(
                                            id = chatUpdate.chatId,
                                            cachePath = avatarCachePath,
                                            avatarHash = null,
                                            nickname = chatUpdate.roomState.subject,
                                            avatarUrl = chatUpdate.roomState.avatarUrl
                                        )
                                    )
                                    chatUpdate.roomState.participants.forEach { (participantId, participantState) ->
                                        userRepository.getAndSaveUser(participantId)
                                        participantDao.insertParticipants(
                                            ParticipantEntity(
                                                chatId = chatUpdate.chatId,
                                                userId = participantId,
                                                role = participantState.role
                                            )
                                        )
                                    }
                                    chatUpdate
                                }

                                else -> {
                                    chatUpdate
                                }
//                                }
                            }
//                        }.await()
                        }
                }
            }

    private suspend fun cacheAvatarImage(avatarUrl: String, chatId: String): String? {
        try {
            return fileRepository.downloadAndSaveToInternalStorage(avatarUrl, chatId).toString()
        } catch (e: Exception) {
            Logger.e(e)
        }
        return null
    }

}

