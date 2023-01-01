package com.hanialjti.allchat.data.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hanialjti.allchat.data.model.User
import com.hanialjti.allchat.presentation.chat.defaultName
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

@Entity(tableName = "users", indices = [Index("external_id", unique = true)])
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "user_id")
    val id: Long = 0,
    @ColumnInfo(name = "external_id")
    val externalId: String,
    @ColumnInfo(name = "owner_id")
    val ownerId: String? = null,
    val name: String? = defaultName,
    val image: String? = null,
    @ColumnInfo(name = "is_online")
    val isOnline: Boolean = false,
    @ColumnInfo(name = "last_online")
    val lastOnline: OffsetDateTime? = null,
    val status: String? = null
)

fun UserEntity.asUser() = User(
    id = externalId,
    name = name,
    image = image,
    isOnline = isOnline,
    lastOnline = lastOnline?.toLocalDateTime()
)