package com.hanialjti.allchat.data.local.room.model

import androidx.room.Embedded
import androidx.room.Relation
import com.hanialjti.allchat.data.local.room.entity.AvatarEntity
import com.hanialjti.allchat.data.local.room.entity.UserEntity

data class UserEntry(
    @Embedded
    val user: UserEntity,

    @Relation(parentColumn = "avatar", entityColumn = "id")
    val avatar: AvatarEntity? = null,
)

