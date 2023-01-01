package com.hanialjti.allchat.data.repository

import com.hanialjti.allchat.common.exception.NotAuthenticatedException
import com.hanialjti.allchat.data.local.datastore.LoggedInUser
import com.hanialjti.allchat.data.local.datastore.UserCredentials
import com.hanialjti.allchat.data.local.datastore.UserPreferencesManager
import com.hanialjti.allchat.data.local.room.dao.UserDao
import com.hanialjti.allchat.data.model.User
import com.hanialjti.allchat.data.model.toUserEntity
import com.hanialjti.allchat.data.remote.model.CallResult
import com.hanialjti.allchat.data.remote.ConnectionManager
import com.hanialjti.allchat.data.remote.UserRemoteDataSource
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber

class UserRepository constructor(
    private val localUserDataSource: UserDao,
    private val remoteDataSource: UserRemoteDataSource,
    private val connectionManager: ConnectionManager,
    private val userPreferencesManager: UserPreferencesManager
) {

    val connectionStatus = connectionManager.observeConnectivityStatus()
    val loggedInUser = userPreferencesManager.loggedInUser

    val connectedUser = connectionStatus.combine(loggedInUser) { connectionStatus, user ->
        return@combine if (connectionStatus == ConnectionManager.Status.Connected) {
            user
        } else null
    }

    suspend fun login(userCredentials: UserCredentials? = null) {

        val credentials = userCredentials ?: userPreferencesManager.userCredentials.firstOrNull()
        ?: throw NotAuthenticatedException("not authenticated", null)

        return connectionManager.connect(credentials).apply {
            if (userCredentials != null) {
                userPreferencesManager.updateUserCredentials(userCredentials)
            }
            saveUserInfoInPreferences()
        }
    }

    private suspend fun saveUserInfoInPreferences() {
        connectionManager.getUsername()?.let {
            var user = LoggedInUser(id = it)
            when (val result = remoteDataSource.getUpdatedUserInfo(it)) {
                is CallResult.Success -> {
                    result.data?.let { updatedUser ->
                        user = user.copy(
                            name = updatedUser.name,
                            image = updatedUser.image
                        )
                    }
                }
                is CallResult.Error -> {
                    Timber.d(result.message)
                }
            }
            userPreferencesManager.updateLoggedInUser(user)
        }
    }

    suspend fun disconnect() {
        connectionManager.disconnect()
    }

    fun getAllUsersByOwnerId(ownerId: String) = localUserDataSource.getByOwnerId(ownerId)


    fun user(userId: String) = localUserDataSource.getById(userId)

//    suspend fun updateAndInsertUser(user: User) {
//        val updatedInfo = remoteDataSource.getUpdatedUserInfo(user.id)
//        var updatedUser = user.toUserEntity()
//        if (updatedInfo is CallResult.Success) {
//            updatedUser = updatedInfo.data ?: user.toUserEntity()
//        }
//        localUserDataSource.updateUser(updatedUser)
//    }

    suspend fun getUsers(userIds: List<String>) = localUserDataSource.getUsers(userIds)
}