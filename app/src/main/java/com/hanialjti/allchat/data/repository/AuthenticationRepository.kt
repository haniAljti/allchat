package com.hanialjti.allchat.data.repository

import com.hanialjti.allchat.data.local.datastore.UserCredentials
import com.hanialjti.allchat.data.remote.ConnectionManager
import com.hanialjti.allchat.data.remote.model.CallResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class AuthenticationRepository(
    private val connectionManager: ConnectionManager,
    private val preferencesRepository: PreferencesRepository,
    private val infoRepository: InfoRepository,
    private val dispatcher: CoroutineDispatcher,
    private val externalScope: CoroutineScope
) {

    private val connectionStatus = connectionManager.observeConnectivityStatus()
    val loggedInUserStream = preferencesRepository.loggedInUserStream

    init {
        externalScope.launch {
            preferencesRepository.clientPreferencesStream
                .collectLatest {
                    connectionManager.updateSendPresences(it.sendPresenceOnLogin)
                }
        }
    }

    val connectedUser = connectionStatus.combine(loggedInUserStream) { connectionStatus, user ->
        return@combine if (connectionStatus == ConnectionManager.Status.Connected) {
            user
        } else null
    }

    suspend fun connectAndDelayRetry(maxRetryCount: Int) {
        val credentials = preferencesRepository.userCredentials() ?: return
        connectionManager.connectAndDelayRetry(credentials, maxRetryCount)
    }

    suspend fun onResume() {
        connectAndDelayRetry(Int.MAX_VALUE)
        connectionManager.onResume()
    }

    suspend fun onPause() {
        connectionManager.onPause()
    }

    suspend fun login(userCredentials: UserCredentials): CallResult<Boolean> =
        connectionManager.login(userCredentials).apply {
            if (this is CallResult.Success) {
                preferencesRepository.updateUserCredentials(userCredentials)
                val loggedInUser = connectionManager.userId
                preferencesRepository.updateLoggedInUser(loggedInUser)
                connectionManager.userId?.let { infoRepository.fetchAndSaveInfo(it, false) }
            }
        }


    suspend fun disconnect() {
        connectionManager.disconnect()
    }
}