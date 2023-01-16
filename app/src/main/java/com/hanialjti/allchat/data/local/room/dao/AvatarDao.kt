package com.hanialjti.allchat.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import com.hanialjti.allchat.data.local.room.entity.AvatarEntity

@Dao
interface AvatarDao {

    @Insert(onConflict = REPLACE)
    suspend fun insertAvatar(avatarEntity: AvatarEntity): Long

}