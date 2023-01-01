package com.hanialjti.allchat.data.remote.model

sealed class CallResult<out T: Any?> {
    data class Success<out T : Any?>(val data: T? = null) : CallResult<T>()
    data class Error(val message: String, val cause: Exception? = null) : CallResult<Nothing>()
}