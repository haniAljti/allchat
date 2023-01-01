package com.hanialjti.allchat.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.map
import androidx.room.withTransaction
import com.hanialjti.allchat.common.model.ListChange
import com.hanialjti.allchat.data.local.room.AllChatLocalRoomDatabase
import com.hanialjti.allchat.data.local.room.entity.ChatEntity
import com.hanialjti.allchat.data.local.room.entity.ParticipantEntity
import com.hanialjti.allchat.data.local.room.entity.UserEntity
import com.hanialjti.allchat.data.local.room.model.asContact
import com.hanialjti.allchat.data.remote.model.CallResult
import com.hanialjti.allchat.data.remote.ChatRemoteDataSource
import com.hanialjti.allchat.data.remote.ConnectionManager
import com.hanialjti.allchat.data.remote.model.RemoteUserItem
import com.hanialjti.allchat.data.remote.model.toUserEntity
import com.hanialjti.allchat.data.tasks.ConversationTasksDataStore
import kotlinx.coroutines.flow.map
import timber.log.Timber

class ConversationRepository constructor(
    private val localDataStore: AllChatLocalRoomDatabase,
    private val remoteDataStore: ChatRemoteDataSource,
    private val tasksDataStore: ConversationTasksDataStore,
    private val connectionManager: ConnectionManager
) {

    private val conversationDao = localDataStore.conversationDao()
    private val userDao = localDataStore.userDao()

    fun contacts(owner: String) = Pager(
        config = PagingConfig(pageSize = 30),
        pagingSourceFactory = { conversationDao.getContacts(owner) }
    ).flow.map {
        it.map { contactEntry ->
            contactEntry.asContact()
        }
    }

    fun contact(conversationId: String, owner: String) =
        conversationDao.getContact(conversationId, owner)?.asContact()

    suspend fun resetUnreadCounter(conversationId: String) {
        conversationDao.resetUnreadCounter(conversationId)
    }

//    suspend fun getRoomInfo(roomId: String) = remoteDataStore.getRoomInfo(roomId)

    suspend fun addChatRoomToRemoteContacts(roomExternalId: String): CallResult<String> {

        val username =
            connectionManager.getUsername() ?: return CallResult.Error("user is not logged in!")

        val conversationEntity = contact(roomExternalId, username)
            ?: return CallResult.Error("conversation with id $roomExternalId could not be found.")

        return conversationEntity.name?.let { nickName ->
            remoteDataStore.createChatRoom(nickName, username)
        } ?: CallResult.Error("Room must have a name")
    }

    suspend fun createChatRoom(
        roomName: String,
        myId: String
    ) {

//        val conversation = ChatEntity(
//            isGroupChat = true,
//            participants = listOf(myId),
//            admins = listOf(myId),
//            name = roomName,
//            owner = myId
//        )

//        val chatRoomLocalId = conversationDao.insert(conversation)[0]
//
//        tasksDataStore.createChatRoom(chatRoomLocalId.toInt())
//        val result = remoteDataStore.createChatRoom(roomName, myId)
//        if (result is CallResult.Success && result.value != null) {
//            conversationDao.insert(
//                ConversationEntity(
//                    id = result.value,
//                    isGroupChat = true,
//
//                )
//            )
//        }
    }

    suspend fun inviteUserToChatRoom(userId: String, conversationId: String, myId: String) {
        remoteDataStore.inviteUserToChatRoom(userId, conversationId, myId)
    }

    suspend fun insert(conversation: ChatEntity) {
        conversationDao.insertOrIgnore(conversation)
    }

//    suspend fun insertConversationAndUser(conversationAndUser: ConversationAndUser) =
//        withContext(Dispatchers.IO) {
//            localDataStore.withTransaction {
//                conversationDao.upsert(conversationAndUser.conversationEntity)
//                conversationAndUser.user?.let { user -> userDao.upsert(user) }
//            }
//            conversationAndUser.conversationEntity.from?.let {
//                startConversation(
//                    conversationAndUser.conversationEntity.id,
//                    conversationAndUser.name,
//                    conversationAndUser.conversationEntity.isGroupChat,
//                    it
//                )
//            }
//        }

    suspend fun syncChats() {
        connectionManager.getUsername()?.let { owner ->
            val chats = remoteDataStore.retrieveContacts()
            chats.forEach { remoteChat ->
                localDataStore.withTransaction {
                    val chatEntity = ChatEntity(
                        externalId = remoteChat.id,
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
                                    externalId = remoteChat.id,
                                    ownerId = owner,
                                    name = remoteChat.name
                                )
                            )
                        }

                        if (conversationDao.getParticipantCountForChat(remoteChat.id, owner) == 0) {
                            conversationDao.insertParticipants(
                                ParticipantEntity(remoteChat.id, owner, remoteChat.id)
                            )
                        }

                    }
                }
            }
        } ?: Timber.e("User is not logged in!")
    }

    suspend fun addContactEntry(
        userId: String,
        name: String,
        image: String?,
        owner: String,
    ): CallResult<String> {
        val conversationEntity = ChatEntity(
            externalId = userId,
            isGroupChat = false,
            name = name,
            image = image,
            owner = owner
        )

        conversationDao.insert(conversationEntity)

        return remoteDataStore.addUserToContact(userId, name)
    }

//    private suspend fun startConversation(
//        conversationId: String,
//        name: String?,
//        isGroupChat: Boolean,
//        myId: String
//    ) = withContext(Dispatchers.IO) {
//
//        val conversationEntity = ConversationEntity(
//            id = conversationId,
//            isGroupChat = isGroupChat,
//            name = name,
//            from = myId,
//            to = if (!isGroupChat) conversationId else null
//        )
//
//        conversationDao.insert(conversationEntity)
//
//        // TODO: Do this in a worker
//        remoteDb.startConversation(
//            conversationId,
//            name ?: defaultName,
//            myId
//        )
//    }

    suspend fun listenForConversationUpdates() {
        remoteDataStore.listenForChatChanges()
            .collect { rosterChange ->
                Timber.d("New Change to the roster")

                when (val userItem = rosterChange) {
//                    is RemoteUserItem.UserData -> {
//                        userDao.updateUser(userItem.toUserEntity())
//                    }
//                    is RemoteUserItem.UserPresence -> {
//                        userDao.updatePresence(
//                            userId = userItem.id,
//                            isOnline = userItem.isOnline,
//                            lastOnline = userItem.lastOnline,
//                            status = userItem.status
//                        )
//                    }
                }

            }
    }
}