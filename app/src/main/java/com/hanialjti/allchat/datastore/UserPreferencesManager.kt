package com.hanialjti.allchat.datastore

import android.content.Context
import androidx.datastore.dataStore
import com.hanialjti.allchat.models.UserCredentials
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class UserPreferencesManager(
    private val context: Context,
    private val applicationScope: CoroutineScope
) {

    private val Context.dataStore by dataStore(
        fileName = "all-chat-preferences.json",
        serializer = UserPreferencesSerializer()
    )

    private val userPreferences = context.dataStore.data.distinctUntilChanged()

    val userCredentials = userPreferences
            .map { it.userCredentials }
        .distinctUntilChanged()

    val username = userCredentials
            .map { it?.username }
            .distinctUntilChanged()

    suspend fun updateUserCredentials(userCredentials: UserCredentials) = context.dataStore
        .updateData { it.copy(userCredentials = userCredentials) }
}