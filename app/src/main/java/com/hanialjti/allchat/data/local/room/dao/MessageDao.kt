package com.hanialjti.allchat.data.local.room.dao

import androidx.paging.PagingSource
import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import com.hanialjti.allchat.data.local.room.entity.MessageEntity
import com.hanialjti.allchat.data.local.room.entity.hasLesserValueThan
import com.hanialjti.allchat.data.local.room.model.MessageEntry
import com.hanialjti.allchat.data.model.Marker
import com.hanialjti.allchat.data.model.MessageStatus
import kotlinx.coroutines.flow.Flow
import java.time.OffsetDateTime

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE contact_id = :conversation AND owner_id = :owner ORDER BY timestamp DESC")
    fun getMessagesByConversation(conversation: String?, owner: String?): PagingSource<Int, MessageEntry>

    @Delete
    suspend fun deleteOne(message: MessageEntity)

    @Query(
        "UPDATE messages SET status = :messageStatus " +
                "WHERE timestamp <= :timestamp " +
                "AND contact_id = :conversationId " +
                "AND sender_id = owner_id " +
                "AND owner_id = :owner " +
                "AND status >= 3 AND status < :messageStatus"
    )
    suspend fun updateStatusForMessagesBeforeTimestamp(
        messageStatus: MessageStatus,
        timestamp: OffsetDateTime,
        owner: String,
        conversationId: String
    )

    @Query(
        "UPDATE messages SET read = 1 " +
                "WHERE contact_id = :conversationId " +
                "AND owner_id = :owner "
    )
    suspend fun setAllMessagesAsRead(
        owner: String,
        conversationId: String
    )

    @Query("SELECT MAX(status) FROM messages WHERE contact_id = :chatId AND owner_id = :owner AND status >= 3")
    suspend fun getGreatestMessageStatus(chatId: String, owner: String): MessageStatus

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("UPDATE messages SET status = :status WHERE message_id = :messageId")
    suspend fun updateMessageStatus(messageId: Int, status: MessageStatus)

    @Insert(onConflict = REPLACE)
    suspend fun insertOrReplace(message: MessageEntity): Long

    @Transaction
    suspend fun upsertMessageStatus(messageId: Int, status: MessageStatus) {
        val existingMessage = getMessageById(messageId)

        if (existingMessage != null) {
            if (existingMessage.status.hasLesserValueThan(status)) {
                updateMessageStatus(messageId, status)
            }
        }
    }

    @Transaction
    suspend fun upsertMessage(message: MessageEntity) {
        val existingMessage = getMessageByRemoteId(message.externalId)

        if (existingMessage == null) {
            insertOrReplace(message)
        } else {
            val status = if (existingMessage.status.hasLesserValueThan(message.status)) {
                message.status
            } else existingMessage.status

            updateMessage(
                existingMessage.copy(
                    status = status,
                    archiveId = message.archiveId
                )
            )

        }
    }

    @Query("SELECT * FROM messages WHERE status = 0 OR status = 1 AND owner_id = :owner")
    suspend fun getPendingMessagesByOwner(owner: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE external_id = :remoteId")
    suspend fun getMessageByRemoteId(remoteId: String?): MessageEntity?

    @Query("SELECT * FROM messages WHERE external_id = :remoteId")
    suspend fun getMessageEntryByRemoteId(remoteId: String?): MessageEntity?

    @Query("select * from messages WHERE owner_id = :owner ORDER BY message_archive_id desc limit 1")
    suspend fun getMostRecentMessage(owner: String): MessageEntity?

    @Query(
        "SELECT * FROM messages " +
                "WHERE owner_id = :owner " +
                "AND owner_id != sender_id " +
                "And contact_id = :conversation " +
                "ORDER BY timestamp DESC limit 1"
    )
    fun getLastMessageNotSendByOwner(owner: String, conversation: String): Flow<MessageEntity>

    @Query("SELECT * FROM messages WHERE message_id = :messageId")
    suspend fun getMessageById(messageId: Int): MessageEntity?
}