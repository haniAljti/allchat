package com.hanialjti.allchat.data.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hanialjti.allchat.data.model.*
import com.hanialjti.allchat.data.remote.model.RemoteMessage
import java.time.OffsetDateTime

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "message_id")
    val id: Long = 0,
    @ColumnInfo(name = "external_id")
    val externalId: String? = null,
    val body: String? = null,
    val timestamp: OffsetDateTime = OffsetDateTime.now(),
    @ColumnInfo(name = "contact_id")
    val contactId: String? = null,
    @ColumnInfo(name = "sender_id")
    val senderId: String? = null,
    @ColumnInfo(name = "owner_id")
    val ownerId: String? = null,
    val status: MessageStatus = MessageStatus.Pending,
    val read: Boolean = false,
    val type: MessageType? = null,
    @ColumnInfo(name = "message_archive_id")
    val archiveId: String? = null,
    @Embedded(prefix = "att_")
    val attachment: AttachmentEntity? = null,
)

fun MessageEntity.asNetworkMessage() = externalId?.let {
    RemoteMessage(
        id = externalId,
        body = body,
        timestamp = timestamp,
        chatId = contactId,
        sender = senderId,
        messageStatus = status,
        type = type,
        messageArchiveId = archiveId
    )
}
fun MessageEntity.asMessageSummary() = MessageSummary(
    body = body,
    status = status,
    timestamp = timestamp.toLocalDateTime(),
    attachmentType = attachment?.type
)

fun MessageStatus?.hasLesserValueThan(otherStatus: MessageStatus?) =
    (this?.value ?: 0) < (otherStatus?.value ?: 0)