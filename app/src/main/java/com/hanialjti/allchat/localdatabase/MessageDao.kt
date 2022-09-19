package com.hanialjti.allchat.localdatabase

import androidx.paging.PagingSource
import androidx.room.*
import androidx.room.OnConflictStrategy.IGNORE
import androidx.room.OnConflictStrategy.REPLACE
import com.hanialjti.allchat.models.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM Message WHERE conversation = :conversation AND owner = :owner ORDER BY timestamp DESC")
    fun getMessagesByConversation(conversation: String?, owner: String?): PagingSource<Int, Message>

    @Query("SELECT * FROM Message WHERE conversation = :conversation ORDER BY timestamp DESC")
    fun getMessagesByConversation(conversation: String?): PagingSource<Int, Message>

    @Query("SELECT * FROM Message WHERE conversation = :conversation ORDER BY timestamp DESC")
    fun getAllByConversation(conversation: String?): List<Message>

//    @Query("UPDATE Message SET status = :status WHERE messageId = :messageId")
//    suspend fun updateMessageStatus(messageId: String, status: Status)

    @Query(
        "UPDATE Message SET status = :status " +
                "WHERE timestamp < :timestamp " +
                "AND conversation = :conversationId " +
                "AND `from` = owner " +
                "AND owner = :owner " +
                "AND status = 'Sent'"
    )
    suspend fun updateStatusForMessagesBeforeTimestamp(
        status: Status,
        timestamp: Long,
        owner: String,
        conversationId: String
    )

    @Insert(onConflict = IGNORE, entity = Message::class)
    suspend fun insertMessageStatus(status: StatusMessage): Long

    @Query(
        "UPDATE Message SET Status = " +
                "CASE WHEN status = 'Pending' OR status = 'Error' THEN :status " +
                "WHEN status = 'Sent' AND :status = 'Seen' OR :status = 'Received' THEN :status " +
                "WHEN status = 'Received' AND :status = 'Seen' THEN :status " +
                "ELSE status END " +
                "WHERE messageId = :messageId"
    )
    suspend fun updateMessageStatus(messageId: String, status: Status)

    @Insert(onConflict = IGNORE)
    suspend fun insertOrIgnore(message: Message): Long

    @Transaction
    suspend fun upsertMessageStatus(status: StatusMessage) {
        val insertResult = insertMessageStatus(status)

        if (insertResult == -1L) {
            updateMessageStatus(status.id, status.status)
        }
    }

    @Insert(onConflict = IGNORE, entity = Message::class)
    suspend fun insertUpdateMessage(updateMessage: UpdateMessage): Long

    @Update(onConflict = REPLACE, entity = Message::class)
    suspend fun updateUpdateMessage(updateMessage: UpdateMessage)

    @Transaction
    suspend fun upsertMessage(message: Message) {
        val insertResult = insertOrIgnore(message)

        if (insertResult == -1L) {
            updateUpdateMessage(message.toUpdateMessage())
        }
    }

    @Query("SELECT * FROM Message WHERE conversation = :conversation ORDER BY timestamp DESC LIMIT 1 ")
    suspend fun getMostRecentMessage(conversation: String): Message?

    @Query("SELECT * FROM Message WHERE owner = :owner ORDER BY timestamp DESC LIMIT 1 ")
    suspend fun getMostRecentMessageByOwner(owner: String): Message?

    @Query("SELECT * FROM Message WHERE messageId = :messageId")
    fun getMessageFlowById(messageId: String): Flow<Message>

    @Query("SELECT * FROM Message WHERE messageId = :messageId")
    suspend fun getMessageById(messageId: String): Message

    @Query("UPDATE Message SET mediaCacheUri = :cacheUri WHERE messageId = :messageId")
    suspend fun saveContentUri(messageId: String, cacheUri: String)
}