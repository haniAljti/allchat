package com.hanialjti.allchat.data.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hanialjti.allchat.data.local.room.model.UserEntry
import com.hanialjti.allchat.data.model.Avatar
import com.hanialjti.allchat.data.model.User
import com.hanialjti.allchat.presentation.chat.defaultName
import com.hanialjti.allchat.presentation.conversation.ContactImage
import java.time.OffsetDateTime

@Entity(tableName = "users", indices = [Index("id", unique = true)])
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String? = defaultName,
    val avatar: String? = null,
    @ColumnInfo(name = "is_online")
    val isOnline: Boolean = false,
    @ColumnInfo(name = "last_online")
    val lastOnline: OffsetDateTime? = null,
    val status: String? = null
)

fun UserEntity.asUser() = User(
    id = id,
    name = name,
    avatar = avatar?.let { ContactImage.DynamicImage(avatar) }
        ?: ContactImage.DefaultProfileImage(false),
    isOnline = isOnline,
    lastOnline = lastOnline?.toLocalDateTime()
)