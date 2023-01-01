package com.hanialjti.allchat.data.remote.model

data class Presence(
    val type: Type,
    val status: String? = null,

) {
    enum class Type(value: Int) { Unavailable(0), Available(1) }
}
