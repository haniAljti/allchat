package com.hanialjti.allchat.data.local.room.entity

import androidx.room.Entity

@Entity("blocked_users", primaryKeys = ["blocked", "owner"])
data class BlockedUserEntity(
    val blocked: String,
    val owner: String
)
