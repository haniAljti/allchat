package com.hanialjti.allchat.data.model

sealed interface Avatar {
    class Raw(val bytes: ByteArray) : Avatar
    class Url(val imageUrl: String) : Avatar
}