package com.hanialjti.allchat.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.room.withTransaction
import coil.Coil
import coil.request.ImageRequest
import coil.request.ImageResult
import com.hanialjti.allchat.common.model.ListChange
import com.hanialjti.allchat.data.local.FileRepository
import com.hanialjti.allchat.data.local.room.AllChatLocalRoomDatabase
import com.hanialjti.allchat.data.local.room.entity.*
import com.hanialjti.allchat.data.model.Avatar
import com.hanialjti.allchat.data.model.Contact
import com.hanialjti.allchat.data.remote.ChatRemoteDataSource
import com.hanialjti.allchat.data.remote.ConnectionManager
import com.hanialjti.allchat.data.remote.model.CallResult
import com.hanialjti.allchat.data.tasks.ConversationTasksDataStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import timber.log.Timber

class ConversationRepository constructor(
    private val localDataStore: AllChatLocalRoomDatabase,
    private val remoteDataStore: ChatRemoteDataSource,
    private val tasksDataStore: ConversationTasksDataStore,
    private val connectionManager: ConnectionManager,
    private val userRepository: UserRepository,
    private val fileRepository: FileRepository,
    private val externalScope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher
) {

    private val conversationDao = localDataStore.conversationDao()
    private val userDao = localDataStore.userDao()
    private val avatarDao = localDataStore.avatarDao()

    private fun contacts(owner: String) = Pager(
        config = PagingConfig(pageSize = 30),
        pagingSourceFactory = { conversationDao.getContacts(owner) }
    ).flow.map {
        it.map { contactEntry ->
            contactEntry.asContact()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun myContacts(): Flow<PagingData<Contact>> = connectionManager.loggedInUser.flatMapLatest {
        it?.let { contacts(it) } ?: emptyFlow()
    }

    fun startListeners() {
        remoteDataStore.startListeners()
    }

    fun stopListeners() {
        remoteDataStore.stopListeners()
    }

    suspend fun contact(userId: String): Contact? = withContext(dispatcher) {
        return@withContext conversationDao.getContact(userId)?.asContact()
    }


    suspend fun resetUnreadCounter(conversationId: String) {
        conversationDao.resetUnreadCounter(conversationId)
    }

//    suspend fun getRoomInfo(roomId: String) = remoteDataStore.getRoomInfo(roomId)

    suspend fun addChatRoomToRemoteContacts(roomExternalId: String): CallResult<String> {

        val username =
            connectionManager.getUsername() ?: return CallResult.Error("user is not logged in!")

        val conversationEntity = contact(roomExternalId)
            ?: return CallResult.Error("conversation with id $roomExternalId could not be found.")

        return conversationEntity.name?.let { nickName ->
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
        connectionManager.getUsername()?.let { owner ->
            val chats = remoteDataStore.retrieveContacts()
            chats.forEach { remoteChat ->
                localDataStore.withTransaction {
                    val chatEntity = ChatEntity(
                        id = remoteChat.id,
                        owner = owner,
                        isGroupChat = remoteChat.isGroupChat,
                        name = remoteChat.name,
                    )

                    insert(chatEntity)

                    if (remoteChat.isGroupChat) {
//                    val chatRoomInfo = remoteDataStore.getRoomInfo(chat.id)
//                    if (chatRoomInfo is CallResult.Success) {
//                        chatRoomInfo.data
//                    }
                    } else {

                        if (!userDao.exists(remoteChat.id)) {
                            userDao.insertUser(
                                UserEntity(
                                    id = remoteChat.id,
                                    name = remoteChat.name
                                )
                            )
                        }

                        if (conversationDao.getParticipantCountForChat(remoteChat.id) == 0) {
                            conversationDao.insertParticipants(
                                ParticipantEntity(remoteChat.id, remoteChat.id)
                            )
                        }

                    }
                }
            }
        } ?: Timber.e("User is not logged in!")
    }

    suspend fun addUserToContactList(userId: String): CallResult<String> {
        val owner = connectionManager.getUsername()
            ?: return CallResult.Error("User is not signed in")

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

            var user: UserEntity? = userDao.findById(userId)

            val userInfoResult = userRepository.fetchUserInfo(userId)

            if (userInfoResult is CallResult.Error) {
                return@withTransaction null
            }

            val userInfo = (userInfoResult as CallResult.Success).data
                ?: return@withTransaction null

            val avatarUri = when (userInfo.user.avatar) {
                is Avatar.Raw -> fileRepository.writeToInternalStorage(
                    userInfo.user.avatar.bytes,
                    userId
                )
                is Avatar.Url -> fileRepository.writeToInternalStorage(
                    userInfo.user.avatar.imageUrl,
                    userId
                )
                else -> null
            }

            avatarUri?.let {
                val avatarEntity = AvatarEntity(
                    userId = userId,
                    cacheUri = avatarUri.toString(),
                    hash = if (userInfo.user.avatar is Avatar.Raw) userInfo.user.avatar.hash else null
                )
                avatarDao.insertAvatar(avatarEntity)
                avatarEntity
            }

            if (user == null) {

                user = UserEntity(
                    id = userId,
                    name = userInfo.user.name,
                    avatar = avatarUri?.toString(),
                    isOnline = userInfo.presence.isOnline,
                    lastOnline = userInfo.presence.lastOnline,
                    status = userInfo.presence.status
                )

                userDao.insertUser(user)

            }

            chat = ChatEntity(
                id = userId,
                isGroupChat = false,
                name = userInfo.user.name,
                avatar = avatarUri?.toString(),
                owner = owner
            )

            conversationDao.insert(chat)
            return@withTransaction chat
        }

    private suspend fun upsertChatRoom(
        roomAddress: String,
        name: String?,
        avatar: Avatar?,
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

    suspend fun listenForConversationUpdates() {
        val owner = connectionManager.getUsername()
        if (owner != null)
            remoteDataStore
                .chatChanges
                .collect { rosterChange ->
                    Timber.d("New Change to the roster")

                    when (rosterChange) {
                        is ListChange.ItemUpdated, is ListChange.ItemAdded -> {

                            if (!rosterChange.item.isGroupChat) {
                                createUserAndAddToContactList(
                                    rosterChange.item.id,
                                    owner
                                )
                            } else {
                                upsertChatRoom(
                                    rosterChange.item.id,
                                    rosterChange.item.name,
                                    null,
                                    owner
                                )
                            }
                        }
//                        is ListChange.ItemAdded -> {
//                            if (!rosterChange.item.isGroupChat) {
//                                createUserAndAddToContactList(
//                                    rosterChange.item.id,
//                                    owner
//                                )
//                            } else {
//                                upsertChatRoom(
//                                    rosterChange.item.id,
//                                    rosterChange.item.name,
//                                    null,
//                                    owner
//                                )
//                            }
//                        }
                        is ListChange.ItemDeleted -> {
                            //TODO
                        }
                    }

                }
    }
}