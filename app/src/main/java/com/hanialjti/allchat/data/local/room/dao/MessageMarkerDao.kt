package com.hanialjti.allchat.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import com.hanialjti.allchat.data.local.room.entity.MarkerEntity
import com.hanialjti.allchat.data.model.Marker
import java.time.OffsetDateTime

@Dao
interface MessageMarkerDao {

    @Insert(onConflict = REPLACE)
    suspend fun insert(vararg marker: MarkerEntity)

    // marker + 4 so that status on the same level as marker and can be replaced when lesser than marker
    @Query(
        """
            INSERT OR REPLACE INTO markers(user_id, message_id, marker, timestamp)
                SELECT :sender, id, :marker, :timestamp 
                FROM messages m WHERE status >= 3 AND status <= :marker AND owner_id = :owner AND contact_id = :chatId AND timestamp <= :timestamp
        """
    )
    suspend fun insertMarkersForMessagesBefore(
        sender: String,
        marker: Marker,
        timestamp: OffsetDateTime,
        owner: String,
        chatId: String
    )

    @Query("SELECT COUNT(*) FROM markers WHERE marker = :marker AND message_id = :messageId")
    suspend fun getCountForMarker(messageId: String, marker: Marker): Int

//    @Query(
////        """
////        SELECT max(marker)
////        FROM markers m
////        WHERE m.message_id = :messageId
////        GROUP BY m.marker
////        HAVING COUNT(*) = (SELECT COUNT(*) FROM participants WHERE chat_id = :chatId)
////        """
//        """
//          SELECT m.id, mr.marker, max(m.timestamp), count(mr.marker)
//          FROM messages m, markers mr
//          WHERE  m.id = mr.message_id
//          GROUP BY mr.marker, m.id
//          HAVING mr.timestamp = max(mr.timestamp)
//          ORDER by mr.marker LIMIT 2
//        """
//    )
//    suspend fun getLatestMarkersSentByAllParticipants(
//        messageId: String,
//        chatId: String
//    ): MarkerEntity

//    @Query(
//        """
//            SELECT EXISTS(
//              SELECT 1
//              FROM markers m, participants p
//              WHERE COUNT()
//            )
//        """
//    )
//    suspend fun sentByAllParticipants(marker: Marker, messageId: String)
}