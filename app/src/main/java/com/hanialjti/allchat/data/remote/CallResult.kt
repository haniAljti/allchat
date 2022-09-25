package com.hanialjti.allchat.data.remote

sealed class CallResult<out T: Any?> {
    data class Success<out T : Any?>(val value: T? = null) : CallResult<T>()
    data class Error(val message: String, val cause: Exception? = null) : CallResult<Nothing>()
}