package com.hanialjti.allchat.data.remote

import com.hanialjti.allchat.data.model.Avatar
import com.hanialjti.allchat.data.remote.model.CallResult
import com.hanialjti.allchat.data.remote.model.FullRemoteUserInfo
import com.hanialjti.allchat.data.remote.model.UserUpdate
import com.hanialjti.allchat.presentation.conversation.ContactImage
import kotlinx.coroutines.flow.Flow

interface UserRemoteDataSource {
    suspend fun getUpdatedUserInfo(userId: String): CallResult<FullRemoteUserInfo>
    suspend fun updateNickname(username: String): CallResult<Boolean>
    suspend fun updateAvatar(data: ByteArray?): CallResult<Boolean>
    suspend fun updateMyInfo(
        name: String,
        avatar: ContactImage?,
        status: String?
    ): CallResult<Boolean>

    suspend fun fetchAvatar(userId: String, hash: String? = null): CallResult<Avatar?>
    fun usersUpdateStream(): Flow<UserUpdate>
    suspend fun blockUser(userId: String): CallResult<Boolean>
    suspend fun unblockUser(userId: String): CallResult<Boolean>
}