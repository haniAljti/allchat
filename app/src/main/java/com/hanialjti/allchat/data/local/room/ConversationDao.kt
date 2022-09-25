package com.hanialjti.allchat.data.local.room

import androidx.room.*
import androidx.room.OnConflictStrategy.*
import com.hanialjti.allchat.data.local.room.entity.ConversationAndUser
import com.hanialjti.allchat.data.local.room.entity.Conversation
import com.hanialjti.allchat.data.local.room.entity.ConversationInfo
import com.hanialjti.allchat.data.local.room.entity.ConversationParticipants
import com.hanialjti.allchat.common.utils.currentTimestamp
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    @Query("SELECT * FROM Conversation WHERE `from` = :owner ORDER BY lastMessage")
    fun getAllByOwner(owner: String?): Flow<List<Conversation>>

    @Query("SELECT * FROM Conversation WHERE `from` = :owner ORDER BY lastUpdated")
    fun getConversationsAndUsers(owner: String?): Flow<List<ConversationAndUser>>

    @Query("SELECT * FROM Conversation WHERE conversationId = :conversation")
    fun getConversationAndUser(conversation: String?): Flow<ConversationAndUser?>

    @Query("SELECT * FROM Conversation WHERE conversationId = :conversation")
    suspend fun getConversationAndUserById(conversation: String?): ConversationAndUser?

    @Query("SELECT * FROM Conversation WHERE conversationId = :conversation")
    suspend fun getConversationById(conversation: String?): Conversation?

    /**
     * updates last message and increment unread messages counter
     */
    @Query("UPDATE Conversation SET lastMessage = :lastMessage, lastUpdated = :lastUpdated, unreadMessages = unreadMessages + 1 WHERE conversationId = :conversationId")
    suspend fun updateLastMessage(lastMessage: String?, lastUpdated: Long = currentTimestamp, conversationId: String)

    @Query("UPDATE Conversation SET unreadMessages = 0 WHERE conversationId = :conversationId")
    suspend fun resetUnreadCounter(conversationId: String)

    @Update(onConflict = REPLACE, entity = Conversation::class)
    suspend fun updateConversationInfo(conversationInfo: ConversationInfo)

    @Transaction
    @Query("SELECT * FROM Conversation WHERE ConversationId = :conversation")
    fun getConversationWithParticipants(conversation: String): Flow<ConversationParticipants>

    @Query("SELECT * FROM Conversation WHERE ConversationId = :id")
    fun getFlowById(id: String): Flow<Conversation>

    @Insert(onConflict = IGNORE)
    suspend fun insert(vararg conversation: Conversation): List<Long>

    @Update(onConflict = REPLACE)
    suspend fun update(vararg conversation: Conversation)

    @Transaction
    suspend fun upsert(vararg conversations: Conversation) = insert(*conversations)
        .withIndex()
        .filter { it.value == -1L }
        .forEach {
            val conversation = conversations[it.index]
            updateConversationInfo(
                ConversationInfo(
                    id = conversation.id,
                    name = conversation.name,
                    imageUrl = conversation.imageUrl,
                    from = conversation.from
                )
            )
        }

}