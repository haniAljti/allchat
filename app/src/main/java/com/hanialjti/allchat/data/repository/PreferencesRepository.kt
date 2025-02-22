package com.hanialjti.allchat.data.repository

import com.hanialjti.allchat.data.local.datastore.ClientPreferences
import com.hanialjti.allchat.data.local.datastore.LoggedInUser
import com.hanialjti.allchat.data.local.datastore.PreferencesLocalDataStore
import com.hanialjti.allchat.data.local.datastore.UserCredentials
import com.hanialjti.allchat.di.preferencesModule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last

class PreferencesRepository(
    private val preferencesLocalDataStore: PreferencesLocalDataStore,
) {

    val clientPreferencesStream = preferencesLocalDataStore.clientPreferences
    val userCredentialsStream = preferencesLocalDataStore.userCredentials
    val loggedInUserStream = preferencesLocalDataStore.usernameStream
    val isSynced = preferencesLocalDataStore.isChatsSyncedStream

    suspend fun loggedInUser() = loggedInUserStream.first()
    suspend fun isChatsSynced() = isSynced.first()
    suspend fun userCredentials() = userCredentialsStream.first()
    suspend fun clientPreferences() = clientPreferencesStream.first()

    suspend fun updateClientPreferences(clientPreferences: ClientPreferences) {
        preferencesLocalDataStore.updateClientPreferences(clientPreferences)
    }

    suspend fun updatePresenceStatus(presenceStatus: String?) {
        preferencesLocalDataStore.updateClientPreferences(
            clientPreferencesStream.first().copy(presenceStatus = presenceStatus)
        )
    }

    suspend fun updateUserCredentials(userCredentials: UserCredentials?) {
        preferencesLocalDataStore.updateUserCredentials(userCredentials)
    }

    suspend fun updateLoggedInUser(loggedInUser: String?) {
        preferencesLocalDataStore.updateLoggedInUser(loggedInUser)
    }

    suspend fun updateIsSynced(isSynced: Boolean) {
        preferencesLocalDataStore.updateIsSynced(isSynced)
    }

}