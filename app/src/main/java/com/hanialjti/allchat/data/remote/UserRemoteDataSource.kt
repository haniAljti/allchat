package com.hanialjti.allchat.data.remote

import com.hanialjti.allchat.data.remote.model.CallResult
import com.hanialjti.allchat.data.remote.model.RemoteUser

interface UserRemoteDataSource {
    suspend fun getUpdatedUserInfo(userId: String): CallResult<RemoteUser>
}