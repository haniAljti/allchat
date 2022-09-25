package com.hanialjti.allchat.common.model

sealed class Resource<out T>(val data: T?) {
    class Success<T>(data: T?): Resource<T>(data)
    class Loading<T>(data: T? = null): Resource<T>(data)
    class Error<T>(message: String? = null, val cause: Throwable?): Resource<T>(null)
}
