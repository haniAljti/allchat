package com.hanialjti.allchat.models

import androidx.room.Embedded
import androidx.room.Ignore
import androidx.room.Relation
import com.hanialjti.allchat.models.entity.Conversation
import com.hanialjti.allchat.models.entity.User

data class ConversationAndUser(
    @Embedded val conversation: Conversation,
    @Relation(
        parentColumn = "to",
        entityColumn = "userId"
    )
    val user: User?
) {
    @Ignore
    val image = if (conversation.isGroupChat) conversation.imageUrl else user?.image

    @Ignore
    val name = if (conversation.isGroupChat) conversation.name else user?.name
}
