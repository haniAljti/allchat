package com.hanialjti.allchat.data.model

sealed interface Avatar {
    class Raw(val bytes: ByteArray, val hash: String) : Avatar
    class Url(val imageUrl: String) : Avatar
}