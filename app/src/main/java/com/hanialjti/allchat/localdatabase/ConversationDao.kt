package com.hanialjti.allchat.localdatabase

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.Transaction
import com.hanialjti.allchat.models.ConversationAndUser
import com.hanialjti.allchat.models.entity.Conversation
import com.hanialjti.allchat.models.entity.ConversationParticipants
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    @Query("SELECT * FROM Conversation WHERE `from` = :owner ORDER BY lastMessage")
    fun getAllByOwner(owner: String?): Flow<List<Conversation>>

    @Query("SELECT * FROM Conversation WHERE `from` = :owner ORDER BY lastUpdated")
    fun getConversationsAndUsers(owner: String?): PagingSource<Int, ConversationAndUser>

    @Query("SELECT * FROM Conversation WHERE conversationId = :conversation")
    fun getConversationAndUser(conversation: String?): Flow<ConversationAndUser>

    @Transaction
    @Query("SELECT * FROM Conversation WHERE ConversationId = :conversation")
    fun getConversationWithParticipants(conversation: String): Flow<ConversationParticipants>

    @Query("SELECT * FROM Conversation WHERE ConversationId = :id")
    fun getFlowById(id: String): Flow<Conversation>

    @Insert(onConflict = REPLACE)
    fun insert(vararg conversation: Conversation)
}