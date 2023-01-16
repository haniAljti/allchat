package com.hanialjti.allchat.data.remote

import com.hanialjti.allchat.data.remote.model.CallResult
import com.hanialjti.allchat.data.remote.model.FullUserInfo
import kotlinx.coroutines.flow.Flow

interface UserRemoteDataSource {
    suspend fun getUpdatedUserInfo(userId: String): CallResult<FullUserInfo>
    suspend fun updateUserInfo(username: String): CallResult<Boolean>
    suspend fun listenForUsernameUpdates(): Flow<String>
}