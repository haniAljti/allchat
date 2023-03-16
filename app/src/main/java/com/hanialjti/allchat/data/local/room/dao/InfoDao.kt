package com.hanialjti.allchat.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import com.hanialjti.allchat.data.local.room.entity.InfoEntity

@Dao
interface InfoDao {

    @Insert(onConflict = REPLACE)
    suspend fun insert(vararg infoEntity: InfoEntity)

    @Query("SELECT * FROM entity_info WHERE avatar_hash = :hash AND id = :id")
    suspend fun getByHashAndId(hash: String, id: String): InfoEntity?

    @Query("UPDATE entity_info SET avatar_path = :path, avatar_hash = :hash WHERE id = :id")
    suspend fun updateAvatar(path: String?, hash: String?, id: String)

    @Query("UPDATE entity_info SET nickname = :nickname WHERE id = :id")
    suspend fun updateNickname(nickname: String?, id: String)

    @Query("SELECT * FROM entity_info WHERE id = :id")
    suspend fun getOne(id: String): InfoEntity?
}