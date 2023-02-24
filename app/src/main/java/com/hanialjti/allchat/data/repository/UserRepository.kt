package com.hanialjti.allchat.data.repository

import com.hanialjti.allchat.common.exception.NotAuthenticatedException
import com.hanialjti.allchat.common.utils.Logger
import com.hanialjti.allchat.data.local.datastore.UserCredentials
import com.hanialjti.allchat.data.local.datastore.PreferencesLocalDataStore
import com.hanialjti.allchat.data.local.room.AllChatLocalRoomDatabase
import com.hanialjti.allchat.data.local.room.entity.BlockedUserEntity
import com.hanialjti.allchat.data.local.room.entity.UserEntity
import com.hanialjti.allchat.data.local.room.entity.asUser
import com.hanialjti.allchat.data.model.User
import com.hanialjti.allchat.data.remote.ConnectionManager
import com.hanialjti.allchat.data.remote.UserRemoteDataSource
import com.hanialjti.allchat.data.remote.model.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.util.*

class UserRepository(
    private val localDb: AllChatLocalRoomDatabase,
    private val remoteDataSource: UserRemoteDataSource,
    private val connectionManager: ConnectionManager,
    private val preferencesLocalDataStore: PreferencesLocalDataStore
) {

    private val userDao = localDb.userDao()
    private val blockedUserDao = localDb.blockedUserDao()

    private val connectionStatus = connectionManager.observeConnectivityStatus()
    val loggedInUser = preferencesLocalDataStore.usernameStream

    val connectedUser = connectionStatus.combine(loggedInUser) { connectionStatus, user ->
        return@combine if (connectionStatus == ConnectionManager.Status.Connected) {
            user
        } else null
    }

    suspend fun getAndSaveUser(userId: String): User {
        var user: UserEntity? = userDao.findById(userId)

        if (user != null) {
            return user.asUser()
        }

        user = UserEntity(id = userId)
        userDao.insertUser(user)

        return user.asUser()
    }

    suspend fun listenForUserUpdates() {
        Logger.d { "Listening for user updates" }
        remoteDataSource.listenForUserUpdates()
            .collect { userUpdate ->
                when (userUpdate) {
//                    is NicknameUpdate -> {
//                        Logger.d { "New nickname update from ${userUpdate.userId} is ${userUpdate.nickname}" }
//                        localDb.withTransaction {
//                            userDao.updateUserName(userUpdate.nickname, userUpdate.userId)
//                            chatDao.updateName(userUpdate.nickname, userUpdate.userId)
//                            messageDao.updateSenderName(userUpdate.nickname, userUpdate.userId)
//                        }
//                    }
//                    is AvatarUrlUpdate -> {
//
//                    }
                    is PresenceUpdate -> {
                        userDao.updatePresence(
                            userId = userUpdate.userId,
                            isOnline = userUpdate.presence.isOnline,
                            lastOnline = userUpdate.presence.lastOnline,
                            status = userUpdate.presence.status
                        )
                    }
                    is NewUserUpdate -> {

                    }
//                    is AvatarMetaDataUpdate -> {
//                        Logger.d { "New avatar update from ${userUpdate.userId} is ${userUpdate.hash}" }
//                        val existingAvatar =
//                            avatarDao.getAvatarByHashAndUserId(userUpdate.hash, userUpdate.userId)
//                        if (existingAvatar == null) {
//                            val response =
//                                remoteDataSource.fetchAvatarData(userUpdate.userId, userUpdate.hash)
//                            if (response is CallResult.Success) {
//                                response.data?.let {
//                                    //TODO use a worker
//                                    val decodedData = Base64.getDecoder().decode(it)
//                                    val imageFile = fileRepository.createNewAvatarFile("${userUpdate.userId}.png")
//                                    val uri = fileRepository.downloadAndSaveToInternalStorage(
//                                        decodedData,
//                                        imageFile
//                                    )?.toString() ?: return@let
//
//                                    avatarDao.insertAvatar(
//                                        InfoEntity(userUpdate.userId, uri, userUpdate.hash)
//                                    )
//                                    chatDao.updateAvatar(uri, userUpdate.userId)
//                                }
//                            }
//                        }
//                    }
                }
            }
    }

    suspend fun blockUser(userId: String) {
        val owner = preferencesLocalDataStore.username() ?: return
        val blockResult = remoteDataSource.blockUser(userId)
        if (blockResult is CallResult.Success) {
            blockedUserDao.insertBlockedUser(BlockedUserEntity(userId, owner))
        }
    }

    suspend fun unblockUser(userId: String) {
        val owner = preferencesLocalDataStore.username() ?: return
        val blockResult = remoteDataSource.unblockUser(userId)
        if (blockResult is CallResult.Success) {
            blockedUserDao.removeBlockedUser(BlockedUserEntity(userId, owner))
        }
    }

    fun isBlocked(userId: String): Flow<Boolean> = loggedInUser.transform { owner ->
        if (owner != null)
            blockedUserDao.fetchBlockedUserFlow(userId, owner)
                .collect { emit(it != null) }
        else emptyFlow<Boolean>()
    }

    suspend fun updateUserNickname(username: String) {
        remoteDataSource.updateNickname(username)
    }

    suspend fun updateAvatar(avatar: ByteArray?) {
        remoteDataSource.updateAvatar(avatar)
    }

    suspend fun login(userCredentials: UserCredentials? = null) {

        val credentials = userCredentials ?: preferencesLocalDataStore.userCredentials.firstOrNull()
        ?: throw NotAuthenticatedException("not authenticated", null)

        connectionManager.connect(credentials)

        if (userCredentials != null) {
            preferencesLocalDataStore.updateUserCredentials(userCredentials)
        }

        val loggedInUser = connectionManager.userId

        preferencesLocalDataStore.updateLoggedInUser(loggedInUser)
    }

//    private suspend fun saveUserInfoInPreferences() {
//        connectionManager.getUsername()?.let {
//            var user = LoggedInUser(id = it)
//            when (val result = remoteDataSource.getUpdatedUserInfo(it)) {
//                is CallResult.Success -> {
//                    result.data?.let { updatedUser ->
//                        user = user.copy(
//                            name = updatedUser.user.name,
//                            image = updatedUser.user.avatar?.data
//                        )
//                    }
//                }
//                is CallResult.Error -> {
//                    Timber.d(result.message)
//                }
//            }
//
//        }
//    }

    suspend fun disconnect() {
        connectionManager.disconnect()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getAllUsersByOwnerId() = loggedInUser.flatMapLatest {
        it?.let {
            userDao
                .getAllByOwnerId(it)
                .map { allUsers ->
                    allUsers.map { userEntry -> userEntry.asUser() }
                }
        } ?: emptyFlow()
    }


    suspend fun user(userId: String) = userDao.getById(userId)?.asUser()

//    suspend fun updateAndInsertUser(user: User) {
//        val updatedInfo = remoteDataSource.getUpdatedUserInfo(user.id)
//        var updatedUser = user.toUserEntity()
//        if (updatedInfo is CallResult.Success) {
//            updatedUser = updatedInfo.data ?: user.toUserEntity()
//        }
//        localUserDataSource.updateUser(updatedUser)
//    }

    suspend fun getUsers(userIds: List<String>) = userDao.getUsers(userIds)
}