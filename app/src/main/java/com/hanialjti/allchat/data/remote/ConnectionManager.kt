package com.hanialjti.allchat.data.remote

import androidx.work.ListenableWorker
import com.hanialjti.allchat.common.exception.NotAuthenticatedException
import com.hanialjti.allchat.data.local.datastore.UserCredentials
import com.hanialjti.allchat.data.remote.model.Presence
import com.hanialjti.allchat.data.remote.xmpp.model.ConnectionConfig
import com.hanialjti.allchat.data.remote.xmpp.model.XmppConnectionConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

interface ConnectionManager {
    val loggedInUser: Flow<String?>

    fun observeConnectivityStatus(): Flow<Status>
    fun getUsername(): String?
    fun getConfig(): ConnectionConfig
    suspend fun connect(userCredentials: UserCredentials)
    suspend fun disconnect()
    suspend fun registerWorker(worker: ListenableWorker)
    suspend fun unregisterWorker(worker: ListenableWorker)
    suspend fun updateMyPresence(presence: Presence)

    suspend fun onResume()
    suspend fun onPause()

    enum class Status { Connected, Disconnected }
}

suspend fun <T> delayRetry(
    func: suspend () -> T?,
    onError: suspend (Throwable) -> Unit,
    breakWhen: () -> Boolean,
    delayInMillis: Long = 30 * 1000,
    maxRetryCount: Int = 5,
    multiplier: Int = 1
): T? {
    while (!breakWhen() && maxRetryCount != 0) {
        try {
            return func()
        } catch (e: Exception) {
            onError(e)
            delay(delayInMillis)
            delayRetry(func, onError, breakWhen, delayInMillis * multiplier, maxRetryCount - 1, multiplier)
        }
    }
    return null
}

suspend inline fun <T> ConnectionManager.registerWorker(
    worker: ListenableWorker,
    work: () -> T?
): T? {
    return try {
        registerWorker(worker)
        work()
    } catch (e: NotAuthenticatedException) {
        Timber.e(e)
        throw e
    } finally {
        unregisterWorker(worker)
    }

}

sealed class ConnectionType {
    class Xmpp(val connectionCredentials: XmppConnectionConfig) : ConnectionType()
    class Firebase() : ConnectionType()
}