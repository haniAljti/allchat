package com.hanialjti.allchat.data.repository

import com.hanialjti.allchat.data.local.room.entity.UserEntity
import com.hanialjti.allchat.data.model.User
import com.hanialjti.allchat.data.model.UserDetails
import com.hanialjti.allchat.data.remote.model.CallResult
import com.hanialjti.allchat.data.remote.model.UserUpdate
import com.hanialjti.allchat.presentation.conversation.ContactImage
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun getAndSaveUser(userId: String? = null)
    suspend fun getUserDetails(userId: String?): UserDetails?
    suspend fun userUpdatesStream(): Flow<UserUpdate>
    suspend fun blockUser(userId: String)
    suspend fun unblockUser(userId: String)
    fun isBlocked(userId: String): Flow<Boolean>
    suspend fun updateMyInfo(
        name: String,
        avatar: ContactImage?,
        status: String?
    ): CallResult<Boolean>
    fun getAllUsers(): Flow<List<User>>
    suspend fun getUsers(userIds: List<String>): List<UserEntity>
}