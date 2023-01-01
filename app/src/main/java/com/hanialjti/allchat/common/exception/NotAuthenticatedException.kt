package com.hanialjti.allchat.common.exception

class NotAuthenticatedException(override val message: String?, override val cause: Throwable?): Exception(message, cause)