package com.hanialjti.allchat.models

data class User(
    val id: String,
    val name: String? = null,
    val status: String? = null,
    val image: String? = null,
    val lastOnline: Long? = null,
)
