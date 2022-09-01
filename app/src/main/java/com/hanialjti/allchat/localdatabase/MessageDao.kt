package com.hanialjti.allchat.localdatabase

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.hanialjti.allchat.models.entity.Message
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM Message WHERE conversation = :conversation ORDER BY timestamp DESC")
    fun getMessagesByConversation(conversation: String?): PagingSource<Int, Message>

    @Query("SELECT * FROM Message WHERE conversation = :conversation ORDER BY timestamp DESC")
    fun getAllByConversation(conversation: String?): List<Message>

    @Insert
    suspend fun insert(message: Message)

    @Query("SELECT * FROM Message WHERE messageId = :messageId")
    fun getMessageById(messageId: String): Flow<Message>

    @Query("UPDATE Message SET mediaCacheUri = :cacheUri WHERE messageId = :messageId")
    suspend fun saveContentUri(messageId: String, cacheUri: String)
}