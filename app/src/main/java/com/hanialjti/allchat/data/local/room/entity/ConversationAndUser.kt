package com.hanialjti.allchat.data.local.room.entity

import androidx.room.Embedded
import androidx.room.Ignore
import androidx.room.Relation
import com.hanialjti.allchat.data.local.room.entity.Conversation
import com.hanialjti.allchat.data.local.room.entity.User

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
