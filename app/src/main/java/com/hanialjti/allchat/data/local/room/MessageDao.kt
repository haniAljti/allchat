package com.hanialjti.allchat.data.local.room

import androidx.paging.PagingSource
import androidx.room.*
import androidx.room.OnConflictStrategy.IGNORE
import androidx.room.OnConflictStrategy.REPLACE
import com.hanialjti.allchat.data.local.room.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM Message WHERE conversation = :conversation AND owner = :owner ORDER BY timestamp DESC")
    fun getMessagesByConversation(conversation: String?, owner: String?): PagingSource<Int, Message>

    @Query("SELECT * FROM Message WHERE conversation = :conversation ORDER BY timestamp DESC")
    fun getMessagesByConversation(conversation: String?): PagingSource<Int, Message>

    @Query("SELECT * FROM Message WHERE conversation = :conversation ORDER BY timestamp DESC")
    fun getAllByConversation(conversation: String?): List<Message>

    @Query(
        "UPDATE Message SET status = :status " +
                "WHERE timestamp < :timestamp " +
                "AND conversation = :conversationId " +
                "AND `from` = owner " +
                "AND owner = :owner " +
                "AND status = 2 OR status = 3 OR status = 4"
    )
    suspend fun updateStatusForMessagesBeforeTimestamp(
        status: Status,
        timestamp: Long,
        owner: String,
        conversationId: String
    )

    @Update(onConflict = IGNORE)
    suspend fun updateMessage(message: Message)

    @Insert(onConflict = IGNORE, entity = Message::class)
    suspend fun insertMessageStatus(status: StatusMessage): Long

    @Query(
        "UPDATE Message SET status = " +
                "CASE WHEN status > :status THEN status " +
                "ELSE :status END " +
                "WHERE remoteId = :remoteId"
    )
    suspend fun updateMessageStatus(remoteId: String?, status: Status)

    @Insert(onConflict = IGNORE)
    suspend fun insertOrIgnore(message: Message): Long

    @Transaction
    suspend fun upsertMessageStatus(status: StatusMessage) {
        val existingMessage = getMessageByRemoteId(status.remoteId)

        if (existingMessage != null) {
            if (existingMessage.status?.hasLesserValueThan(status.status) == true) {
                updateMessageStatus(status.remoteId, status.status)
            }
        } else {
            insertMessageStatus(status)
        }
    }

    @Insert(onConflict = IGNORE, entity = Message::class)
    suspend fun insertUpdateMessage(updateMessage: UpdateMessage): Long

    @Update(onConflict = REPLACE, entity = Message::class)
    suspend fun updateUpdateMessage(updateMessage: UpdateMessage)

    @Transaction
    suspend fun upsertMessage(message: Message) {
        val existingMessage = getMessageByRemoteId(message.remoteId)

        if (existingMessage == null) {
            insertOrIgnore(message)
        } else {
            updateUpdateMessage(message.toUpdateMessage())
        }
    }

    @Query("SELECT * FROM Message WHERE status = 0 OR status = 1 AND owner = :owner")
    suspend fun getPendingMessagesByOwner(owner: String): List<Message>

    @Query("SELECT * FROM Message WHERE remoteId = :remoteId")
    suspend fun getMessageByRemoteId(remoteId: String?): Message?

    @Query("SELECT * FROM Message WHERE conversation = :conversation AND owner = :owner ORDER BY timestamp DESC LIMIT 1 ")
    suspend fun getMostRecentMessage(conversation: String, owner: String): Message?

    @Query("SELECT * FROM Message WHERE owner = :owner ORDER BY timestamp DESC LIMIT 1 ")
    suspend fun getMostRecentMessage(owner: String): Message?

    @Query("SELECT * FROM Message WHERE messageId = :messageId")
    fun getMessageFlowById(messageId: Int): Flow<Message>

    @Query("SELECT * FROM Message " +
            "WHERE owner = :owner " +
            "AND owner != `from` " +
            "And conversation = :conversation " +
            "ORDER BY timestamp DESC limit 1")
    fun getLastMessageNotSendByOwner(owner: String, conversation: String): Flow<Message>

    @Query("SELECT * FROM Message WHERE messageId = :messageId")
    suspend fun getMessageById(messageId: Int): Message

    @Query("UPDATE Message SET mediaCacheUri = :cacheUri WHERE messageId = :messageId")
    suspend fun saveContentUri(messageId: Int, cacheUri: String)
}