package com.hanialjti.allchat.models.entity

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
