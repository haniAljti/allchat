package com.hanialjti.allchat.data.local.room.dao

import androidx.paging.PagingSource
import androidx.room.*
import androidx.room.OnConflictStrategy.IGNORE
import androidx.room.OnConflictStrategy.REPLACE
import com.hanialjti.allchat.data.local.room.entity.ChatEntity
import com.hanialjti.allchat.data.local.room.entity.ParticipantEntity
import com.hanialjti.allchat.data.local.room.model.ChatEntry
import com.hanialjti.allchat.data.model.MessageSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

//    @Transaction
//    @Query(
//        """
//            SELECT c.id AS id,
//                c.is_group_chat AS isGroupChat,
//                c.name AS name,
//                a.data AS imageData,
//                a.data AS imageData,
//                m.body AS lastMessage,
//                m.timestamp AS lastMessageTimestamp,
//                m.status AS lastMessageStatus,
//                att.type AS lastMessageAttachmentType,
//                (m.owner_id == m.sender_id) as isLastMessageSentByMe,
//                c.unread_messages_count AS unreadMessages
//                FROM chats c
//                LEFT JOIN messages m ON m.contact_id = c.id AND m.timestamp = (select max(timestamp) from messages where c.id = contact_id)
//                LEFT JOIN attachments att ON m.attachment = att.id
//                LEFT JOIN avatars a ON a.id = c.avatar
//                WHERE c.owner = :owner ORDER BY timestamp DESC
//        """
//    )
//    fun getContacts(owner: String): PagingSource<Int, ChatEntry>

    @Query("SELECT * FROM chats WHERE owner = :owner")
    fun getContacts(owner: String): PagingSource<Int, ChatEntity>

    @Query("SELECT * FROM chats WHERE id = :contactId")
    fun getContact(contactId: String): ChatEntity?

//    @Transaction
//    @Query(
//        "SELECT c.id AS id, c.is_group_chat AS isGroupChat, " +
//                "c.name AS name, " +
//                "c.avatar AS image, " +
//                "m.body AS lastMessage, " +
//                "m.timestamp AS lastMessageTimestamp, " +
//                "m.status AS lastMessageStatus, " +
//                "m.att_type AS lastMessageAttachmentType, " +
//                "(m.owner_id == m.sender_id) as isLastMessageSentByMe, " +
//                "c.unread_messages_count AS unreadMessages " +
//                "FROM chats c " +
//                "LEFT JOIN messages m ON m.contact_id = c.id AND m.timestamp = (select max(timestamp) from messages where c.id = contact_id)" +
//                "WHERE c.owner = :owner AND c.id = :contactId"
//    )
//    fun getContact(contactId: String, owner: String): ChatEntry?

    @Query("SELECT * FROM chats WHERE id = :remoteId")
    suspend fun getConversationByRemoteId(remoteId: String?): ChatEntity?

    @Query("UPDATE chats SET lastMessage = :lastMessageSummary WHERE id = :chatId")
    suspend fun updateLastMessage(lastMessageSummary: MessageSummary, chatId: String)

    @Query("UPDATE chats SET unread_messages_count = 0 WHERE id = :conversationId")
    suspend fun resetUnreadCounter(conversationId: String)

    @Query("SELECT * FROM chats WHERE id = :id")
    fun getFlowById(id: String): Flow<ChatEntity>

    @Insert(onConflict = IGNORE)
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