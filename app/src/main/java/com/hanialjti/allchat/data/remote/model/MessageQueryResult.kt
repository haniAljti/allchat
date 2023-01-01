package com.hanialjti.allchat.data.remote.model


sealed interface MessageQueryResult {
    class Success(val isEndOfList: Boolean): MessageQueryResult
    class Error(val cause: Throwable): MessageQueryResult
}