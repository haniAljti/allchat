package com.hanialjti.allchat.data.local.room.dao

import androidx.paging.PagingSource
import androidx.room.*
import androidx.room.OnConflictStrategy.Companion.REPLACE
import com.hanialjti.allchat.data.local.room.entity.ChatEntity
import com.hanialjti.allchat.data.local.room.entity.ParticipantEntity
import com.hanialjti.allchat.data.local.room.model.Chat
import com.hanialjti.allchat.data.local.room.model.ChatWithLastMessage
import com.hanialjti.allchat.data.model.ChatInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    @Query("SELECT * FROM chats WHERE owner = :owner")
    fun getContacts(owner: String): PagingSource<Int, ChatEntity>

    @Transaction
    @Query(
        """
        SELECT c.id                                                          as chat_id,
               c.owner                                                       as chat_owner,
               c.is_group_chat                                               as chat_is_group_chat,
               c.unread_messages_count                                       as chat_unread_messages_count,
               c.participants                                                as chat_participants,
               u.is_online                                                   as user_is_online,
               u.id                                                          as user_id,
               a.nickname                                                    as nickname,
               a.avatar_path                                                 as avatar,
               m.body                                                        as message_body,
               m.status                                                      as message_status,
               m.timestamp                                                   as message_timestamp,
               m.attachment                                                  as message_attachment,
               case when m.sender_id = :owner then 1 else 0 end              as message_sent_by_me
        FROM chats c
                 LEFT JOIN (SELECT *
                            FROM messages
                            WHERE owner_id = :owner
                            GROUP BY contact_id
                            HAVING MAX(timestamp)) m
                           ON m.contact_id = c.id
                 LEFT JOIN entity_info a ON a.id = c.id
                 LEFT JOIN users u ON u.id = c.id
        WHERE owner = :owner
        """
    )
    fun getContactsWithLastMessage(owner: String): PagingSource<Int, ChatWithLastMessage>

    @Transaction
    @Query(
        """
        SELECT c.id                                                          as chat_id,
               c.owner                                                       as chat_owner,
               c.is_group_chat                                               as chat_is_group_chat,
               c.unread_messages_count                                       as chat_unread_messages_count,
               c.participants                                                as chat_participants,
               u.is_online                                                   as user_is_online,
               u.id                                                          as user_id,
               a.nickname                                                    as nickname,
               a.avatar_path                                                 as avatar
        FROM chats c
                 LEFT JOIN entity_info a ON a.id = c.id
                 LEFT JOIN users u ON u.id = c.id
        WHERE c.owner = :owner AND c.id = :chatId
        """
    )
    fun getChatInfo(owner: String, chatId: String): Flow<Chat?>

    @Transaction
    @Query(
        """
        SELECT c.id as chat_id,
         c.owner as chat_owner,
         c.is_group_chat as chat_is_group_chat,
         c.unread_messages_count as chat_unread_messages_count,
         c.participants as chat_participants,
         u.is_online as user_is_online,
         u.id as user_id,
         a.nickname as nickname,
         a.avatar_path as avatar
        FROM chats c,
          users u,
          entity_info a
        WHERE c.id = :contactId AND u.id = c.id AND a.id = c.id
        """
    )
    fun getChatWithLastMessage(contactId: String): Flow<ChatWithLastMessage?>

    @Query("SELECT * FROM chats WHERE id = :contactId")
    suspend fun getContact(contactId: String): ChatEntity?

    @Query("SELECT * FROM chats WHERE id = :remoteId")
    suspend fun getConversationByRemoteId(remoteId: String?): ChatEntity?

    @Query("SELECT * FROM chats WHERE id = :chatId AND owner = :owner")
    suspend fun getOne(chatId: String, owner: String): ChatEntity?

    @Query("UPDATE chats SET unread_messages_count = 0 WHERE id = :conversationId")
    suspend fun resetUnreadCounter(conversationId: String)

    @Query("SELECT * FROM chats WHERE id = :id")
    fun getFlowById(id: String): Flow<ChatEntity>

    @Insert(onConflict = REPLACE)
    suspend fun insert(vararg conversationEntity: ChatEntity): List<Long>

    @Update(onConflict = REPLACE)
    suspend fun update(vararg conversationEntity: ChatEntity)

    @Transaction
    suspend fun insertOrIgnore(conversationEntity: ChatEntity) {
        val existingConversation = getConversationByRemoteId(conversationEntity.id)

        if (existingConversation == null) {
            insert(conversationEntity)
        }
    }

    @Insert
    suspend fun insertParticipants(vararg participant: ParticipantEntity)

    @Query("SELECT COUNT(*) FROM PARTICIPANTS WHERE chat_id = :chatId")
    suspend fun getParticipantCountForChat(chatId: String): Int

//    @Transaction
//    suspend fun upsert(vararg conversationEntities: ChatEntity) = insert(*conversationEntities)
//        .withIndex()
//        .filter { it.value == -1L }
//        .forEach {
//            val conversation = conversationEntities[it.index]
//            updateConversationInfo(
//                ConversationInfoEntity(
//                    id = conversation.id,
//                    externalId = conversation.externalId,
//                    name = conversation.name,
//                    image = conversation.image,
//                    from = conversation.owner
//                )
//            )
//        }

}