package com.hanialjti.allchat.data.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import com.hanialjti.allchat.data.model.Marker
import java.time.OffsetDateTime

@Entity(
    tableName = "markers",
    primaryKeys = ["user_id", "message_id"],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = arrayOf("external_id"),
            childColumns = arrayOf("user_id"),
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MarkerEntity(
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "message_id")
    val messageId: String,
    val marker: Marker,
    val timestamp: OffsetDateTime = OffsetDateTime.now()
)
