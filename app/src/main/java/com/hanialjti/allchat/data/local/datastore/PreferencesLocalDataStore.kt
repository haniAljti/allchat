package com.hanialjti.allchat.data.local.datastore

import android.content.Context
import androidx.datastore.dataStore
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class PreferencesLocalDataStore(
    private val context: Context
) {

    private val Context.dataStore by dataStore(
        fileName = "all-chat-preferences.json",
        serializer = UserPreferencesSerializer()
    )

    private val userPreferences = context.dataStore.data.distinctUntilChanged()

    val userCredentials = userPreferences
        .map { it.userCredentials }
        .distinctUntilChanged()

    val clientPreferences = userPreferences
        .map { it.clientPreferences }
        .distinctUntilChanged()

    val usernameStream = userPreferences
        .map { it.loggedInUser }

    val isChatsSyncedStream = userPreferences
        .map { it.isSynced }

    suspend fun username() = usernameStream.first()

    suspend fun updateUserCredentials(userCredentials: UserCredentials?) = context.dataStore
        .updateData { it.copy(userCredentials = userCredentials) }

    suspend fun updateClientPreferences(clientPreferences: ClientPreferences) = context.dataStore
        .updateData { it.copy(clientPreferences = clientPreferences) }

    suspend fun updateLoggedInUser(user: String?) = context.dataStore
        .updateData { it.copy(loggedInUser = user) }

    suspend fun updateIsSynced(isSynced: Boolean) = context.dataStore
        .updateData { it.copy(isSynced = isSynced) }
}