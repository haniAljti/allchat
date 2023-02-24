package com.hanialjti.allchat.data.local.room.dao

import androidx.room.*
import com.hanialjti.allchat.data.local.room.entity.BlockedUserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedUserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedUser(vararg blockedUserEntity: BlockedUserEntity)

    @Delete
    suspend fun removeBlockedUser(vararg blockedUserEntity: BlockedUserEntity)

    @Query("SELECT * FROM blocked_users where blocked = :userId AND owner = :owner")
    fun fetchBlockedUserFlow(userId: String, owner: String): Flow<BlockedUserEntity?>
}