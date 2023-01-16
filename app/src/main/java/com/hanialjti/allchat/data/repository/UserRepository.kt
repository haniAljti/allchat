package com.hanialjti.allchat.data.repository

import com.hanialjti.allchat.common.exception.NotAuthenticatedException
import com.hanialjti.allchat.data.local.datastore.UserCredentials
import com.hanialjti.allchat.data.local.datastore.UserPreferencesManager
import com.hanialjti.allchat.data.local.room.dao.UserDao
import com.hanialjti.allchat.data.local.room.entity.asUser
import com.hanialjti.allchat.data.remote.ConnectionManager
import com.hanialjti.allchat.data.remote.UserRemoteDataSource
import com.hanialjti.allchat.data.remote.model.CallResult
import com.hanialjti.allchat.data.remote.model.FullUserInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

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

    suspend fun listenForUsername() {
        remoteDataSource.listenForUsernameUpdates()
            .collect()
    }

    suspend fun fetchUserInfo(userId: String): CallResult<FullUserInfo> {
        return remoteDataSource.getUpdatedUserInfo(userId)
    }

    suspend fun updateUserInfo(username: String) {
        remoteDataSource.updateUserInfo(username)
    }

    suspend fun login(userCredentials: UserCredentials? = null) {

        val credentials = userCredentials ?: userPreferencesManager.userCredentials.firstOrNull()
        ?: throw NotAuthenticatedException("not authenticated", null)

        connectionManager.connect(credentials)

        if (userCredentials != null) {
            userPreferencesManager.updateUserCredentials(userCredentials)
        }

        val loggedInUser = connectionManager.getUsername()

        userPreferencesManager.updateLoggedInUser(loggedInUser)
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
    fun getAllUsersByOwnerId() = connectionManager.loggedInUser.flatMapLatest {
        it?.let {
            localUserDataSource
                .getAllByOwnerId(it)
                .map { allUsers ->
                    allUsers.map { userEntry -> userEntry.asUser() }
                }
        } ?: emptyFlow()
    }


    suspend fun user(userId: String) = localUserDataSource.getById(userId)?.asUser()

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