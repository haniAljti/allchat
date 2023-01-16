package com.hanialjti.allchat.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import com.hanialjti.allchat.data.local.room.entity.ParticipantEntity
import com.hanialjti.allchat.data.model.Participant

@Dao
interface ParticipantDao {

    @Insert(onConflict = REPLACE)
    fun insertParticipants(vararg participant: ParticipantEntity)

    @Query("SELECT COUNT(*) FROM participants WHERE chat_id = :chatId")
    fun getParticipantCountForChat(chatId: String): Int

}