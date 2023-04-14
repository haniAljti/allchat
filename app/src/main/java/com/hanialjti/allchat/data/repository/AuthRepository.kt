package com.hanialjti.allchat.data.repository

import com.hanialjti.allchat.data.local.datastore.UserCredentials
import com.hanialjti.allchat.data.remote.model.CallResult
import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    val loggedInUserStream: Flow<String?>

    val connectedUser: Flow<String?>

    suspend fun connectAndDelayRetry(maxRetryCount: Int)

    suspend fun onResume()
    suspend fun onPause()
    suspend fun login(userCredentials: UserCredentials): CallResult<Boolean>
}