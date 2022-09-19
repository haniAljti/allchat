package com.hanialjti.allchat.models

sealed class Resource<out T>(val data: T?) {
    class Success<T>(data: T?): Resource<T>(data)
    class Loading<T>(data: T? = null): Resource<T>(data)
    class Error<T>(data: T? = null, val cause: Throwable?): Resource<T>(data)
}
