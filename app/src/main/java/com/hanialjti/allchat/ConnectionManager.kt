package com.hanialjti.allchat

interface ConnectionManager {
    suspend fun connect(username: String, password: String)
    suspend fun disconnect()

}