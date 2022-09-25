package com.hanialjti.allchat.data.local.room.entity

import androidx.room.Entity

@Entity(primaryKeys = ["conversationId", "userId"])
data class ConversationParticipantsCrossRef(
    val conversationId: String,
    val userId: String,
)