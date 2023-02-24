package com.hanialjti.allchat.data.repository

import com.hanialjti.allchat.data.local.datastore.PreferencesLocalDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last

class PreferencesRepository(
    private val preferencesLocalDataStore: PreferencesLocalDataStore,
) {

    val clientPreferencesStream = preferencesLocalDataStore.clientPreferences
    val userCredentialsStream = preferencesLocalDataStore.userCredentials
    val loggedInUserStream = preferencesLocalDataStore.usernameStream

    suspend fun loggedInUser() = loggedInUserStream.first()
    suspend fun userCredentials() = userCredentialsStream.first()
    suspend fun clientPreferences() = clientPreferencesStream.first()



}