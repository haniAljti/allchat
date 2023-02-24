package com.hanialjti.allchat.data.remote

import com.hanialjti.allchat.data.remote.model.CallResult
import com.hanialjti.allchat.data.remote.model.FullUserInfo
import com.hanialjti.allchat.data.remote.model.UserUpdate
import kotlinx.coroutines.flow.Flow

interface UserRemoteDataSource {
    suspend fun getUpdatedUserInfo(userId: String): CallResult<FullUserInfo>
    suspend fun updateNickname(username: String): CallResult<Boolean>
    suspend fun updateAvatar(data: ByteArray?): CallResult<Boolean>

    suspend fun fetchAvatarData(userId: String, hash: String): CallResult<String?>

    fun listenForUserUpdates(): Flow<UserUpdate>
    suspend fun blockUser(userId: String): CallResult<Boolean>
    suspend fun unblockUser(userId: String): CallResult<Boolean>
}