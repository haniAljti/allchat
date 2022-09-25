package com.hanialjti.allchat.data.local.room.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class ConversationParticipants(
    @Embedded
    val conversation: Conversation,
    @Relation(
        parentColumn = "conversationId",
        entityColumn = "userId",
        associateBy = Junction(ConversationParticipantsCrossRef::class)
    )
    val participants: List<User> = listOf()
)
