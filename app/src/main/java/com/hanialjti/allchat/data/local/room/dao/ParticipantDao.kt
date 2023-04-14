package com.hanialjti.allchat.data.local.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.IGNORE
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import com.hanialjti.allchat.data.local.room.entity.ParticipantEntity
import com.hanialjti.allchat.data.model.Participant
import com.hanialjti.allchat.data.model.Role

@Dao
interface ParticipantDao {

    @Insert(onConflict = REPLACE)
    suspend fun insertParticipants(vararg participant: ParticipantEntity)

    @Query("SELECT COUNT(*) FROM participants WHERE chat_id = :chatId")
    suspend fun getParticipantCountForChat(chatId: String): Int

    @Query("SELECT * FROM participants WHERE chat_id = :chatId AND user_id = :userId")
    suspend fun getOne(chatId: String, userId: String): ParticipantEntity?

    @Query("UPDATE participants SET state = :state WHERE chat_id = :chatId AND user_id = :userId")
    suspend fun updateState(chatId: String, userId: String, state: Participant.State)

    @Query("UPDATE participants SET role = :role WHERE chat_id = :chatId AND user_id = :userId")
    suspend fun updateRole(chatId: String, userId: String, role: Role)

    @Query("DELETE FROM participants WHERE chat_id = :chatId AND user_id = :userId")
    suspend fun deleteOne(chatId: String, userId: String)
}