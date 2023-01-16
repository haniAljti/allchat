package com.hanialjti.allchat.data.local.datastore

import android.content.Context
import androidx.datastore.dataStore
import com.hanialjti.allchat.data.local.room.entity.UserEntity
import com.hanialjti.allchat.data.model.User
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class UserPreferencesManager(
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

    val loggedInUser = userPreferences
        .map { it.loggedInUser }

    val username = userCredentials
        .map { it?.username }
        .distinctUntilChanged()

    suspend fun updateUserCredentials(userCredentials: UserCredentials) = context.dataStore
        .updateData { it.copy(userCredentials = userCredentials) }

    suspend fun updateLoggedInUser(user: String?) = context.dataStore
        .updateData { it.copy(loggedInUser = user) }
}