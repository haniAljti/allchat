package com.hanialjti.allchat.data.repository

import com.hanialjti.allchat.common.utils.Logger
import com.hanialjti.allchat.data.local.room.dao.BlockedUserDao
import com.hanialjti.allchat.data.local.room.dao.UserDao
import com.hanialjti.allchat.data.local.room.entity.BlockedUserEntity
import com.hanialjti.allchat.data.local.room.entity.UserEntity
import com.hanialjti.allchat.data.local.room.entity.asUser
import com.hanialjti.allchat.data.model.User
import com.hanialjti.allchat.data.remote.UserRemoteDataSource
import com.hanialjti.allchat.data.remote.model.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.util.*

class UserRepository(
    private val userDao: UserDao,
    private val blockedUserDao: BlockedUserDao,
    private val remoteDataSource: UserRemoteDataSource,
    private val authenticationRepository: AuthenticationRepository
) {

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
                }
            }
    }

    suspend fun blockUser(userId: String) {
        val owner = authenticationRepository.loggedInUserStream.first() ?: return
        val blockResult = remoteDataSource.blockUser(userId)
        if (blockResult is CallResult.Success) {
            blockedUserDao.insertBlockedUser(BlockedUserEntity(userId, owner))
        }
    }

    suspend fun unblockUser(userId: String) {
        val owner = authenticationRepository.loggedInUserStream.first() ?: return
        val blockResult = remoteDataSource.unblockUser(userId)
        if (blockResult is CallResult.Success) {
            blockedUserDao.removeBlockedUser(BlockedUserEntity(userId, owner))
        }
    }

    fun isBlocked(userId: String): Flow<Boolean> = authenticationRepository
        .loggedInUserStream
        .transform { owner ->
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

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getAllUsersByOwnerId() = authenticationRepository
        .loggedInUserStream
        .flatMapLatest {
            it?.let {
                userDao
                    .getAllByOwnerId(it)
                    .map { allUsers ->
                        allUsers.map { userEntry -> userEntry.asUser() }
                    }
            } ?: emptyFlow()
        }


    suspend fun user(userId: String) = userDao.getById(userId)?.asUser()

    suspend fun getUsers(userIds: List<String>) = userDao.getUsers(userIds)
}