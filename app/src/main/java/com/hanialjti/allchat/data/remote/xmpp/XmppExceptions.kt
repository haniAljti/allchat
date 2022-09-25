package com.hanialjti.allchat.data.remote.xmpp

class ChatRoomCreationException : Exception {
    constructor(message: String, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable) : super("Failed to create chat room", cause)
}

class ChatRoomAlreadyCreatedException(message: String, cause: Throwable? = null) :
    Exception(message, cause)