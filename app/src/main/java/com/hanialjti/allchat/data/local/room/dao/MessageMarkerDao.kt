package com.hanialjti.allchat.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.Transaction
import com.hanialjti.allchat.data.local.room.entity.MarkerEntity
import com.hanialjti.allchat.data.model.Marker
import java.sql.Timestamp
import java.time.OffsetDateTime

@Dao
interface MessageMarkerDao {

    @Insert(onConflict = REPLACE)
    suspend fun insert(vararg marker: MarkerEntity)

    // marker + 4 so that status on the same level as marker and can be replaced when lesser than marker
    @Query(
        "INSERT OR REPLACE INTO markers(user_id, message_id, marker, timestamp) " +
                "SELECT :sender, external_id, :marker, :timestamp " +
                "FROM messages WHERE status >= 3 AND status <= :marker AND owner_id = :owner AND contact_id = :chatId AND timestamp <= :timestamp "
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

}