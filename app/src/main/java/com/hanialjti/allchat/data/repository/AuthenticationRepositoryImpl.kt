package com.hanialjti.allchat.data.repository

import com.hanialjti.allchat.data.local.datastore.UserCredentials
import com.hanialjti.allchat.data.remote.ConnectionManager
import com.hanialjti.allchat.data.remote.model.CallResult
import com.hanialjti.allchat.data.remote.model.Presence
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class AuthenticationRepositoryImpl(
    private val connectionManager: ConnectionManager,
    private val preferencesRepository: PreferencesRepository,
    private val dispatcher: CoroutineDispatcher,
    private val externalScope: CoroutineScope
):AuthRepository {

    private val connectionStatus = connectionManager.observeConnectivityStatus()
    override val loggedInUserStream = preferencesRepository.loggedInUserStream

    init {
        externalScope.launch {
            preferencesRepository.clientPreferencesStream
                .collectLatest {
                    connectionManager.updateSendPresences(it.sendPresenceOnLogin)
                }
        }
    }

    override val connectedUser = connectionStatus.combine(loggedInUserStream) { connectionStatus, user ->
        return@combine if (connectionStatus == ConnectionManager.Status.Connected) {
            user
        } else null
    }

    override suspend fun connectAndDelayRetry(maxRetryCount: Int) {
        val credentials = preferencesRepository.userCredentials() ?: return
        connectionManager.connectAndDelayRetry(credentials, maxRetryCount)
    }

    override suspend fun onResume() {
        connectAndDelayRetry(Int.MAX_VALUE)
//        connectionManager.onResume()
        connectionManager.updateMyPresence(
            Presence(
                Presence.Type.Available,
                preferencesRepository.clientPreferences().presenceStatus
            )
        )
    }

    override suspend fun onPause() {
//        connectionManager.onPause()
        connectionManager.updateMyPresence(
            Presence(
                Presence.Type.Unavailable,
                preferencesRepository.clientPreferences().presenceStatus
            )
        )
    }

    override suspend fun login(userCredentials: UserCredentials): CallResult<Boolean> =
        connectionManager.login(userCredentials).apply {
            if (this is CallResult.Success) {
                preferencesRepository.updateUserCredentials(userCredentials)
                val loggedInUser = connectionManager.userId
                preferencesRepository.updateLoggedInUser(loggedInUser)
//                connectionManager.userId?.let { userRepository.getAndSaveUser(it) }
            }
        }

}