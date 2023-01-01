package com.hanialjti.allchat.data.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import com.hanialjti.allchat.data.model.Contact
import com.hanialjti.allchat.data.model.Role

@Entity(
    tableName = "participants",
    primaryKeys = ["chat_id", "user_id"],
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = arrayOf("external_id", "owner"),
            childColumns = arrayOf("chat_id", "owner"),
            onDelete = CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = arrayOf("external_id"),
            childColumns = arrayOf("user_id"),
            onDelete = CASCADE
        )
    ]
)
data class ParticipantEntity(
    @ColumnInfo(name = "chat_id")
    val chatId: String,
    val owner: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    val state: Contact.State = Contact.State.Inactive,
    val roles: List<Role> = listOf(Role.Participant)
)