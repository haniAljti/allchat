package com.hanialjti.allchat.data.model

data class ChatRoomInfo(
    val id: String,
    val Nickname: String,
    val avatar: String,
    val description: String,
    val participants: List<Participant>
)
