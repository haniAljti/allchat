package com.hanialjti.allchat.data.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import com.hanialjti.allchat.data.model.Participant
import com.hanialjti.allchat.data.model.Role
import java.time.OffsetDateTime

@Entity(
    tableName = "participants",
    primaryKeys = ["chat_id", "user_id"],
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("chat_id")
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("user_id")
        )
    ]
)
data class ParticipantEntity(
    @ColumnInfo(name = "chat_id")
    val chatId: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    val state: Participant.State = Participant.State.Inactive,
    val role: Role = Role.Participant,
    val since: OffsetDateTime = OffsetDateTime.now()
)