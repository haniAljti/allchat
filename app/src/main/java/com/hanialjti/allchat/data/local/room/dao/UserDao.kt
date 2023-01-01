package com.hanialjti.allchat.data.local.room.dao

import androidx.room.*
import com.hanialjti.allchat.data.local.room.entity.UserEntity
import kotlinx.coroutines.flow.Flow
import java.time.OffsetDateTime

@Dao
interface UserDao {

    @Query("SELECT * FROM users WHERE user_id = :id")
    fun getById(id: String): Flow<UserEntity>

    @Query("SELECT * FROM users WHERE owner_id = :ownerId")
    fun getByOwnerId(ownerId: String): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE user_id IN (:ids)")
    suspend fun getUsers(ids: List<String>): List<UserEntity>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateUser(vararg user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUser(vararg user: UserEntity): List<Long>

    @Query("SELECT EXISTS(SELECT * FROM users WHERE external_id = :externalId)")
    suspend fun exists(externalId: String): Boolean

    @Query("UPDATE users SET is_online = :isOnline, last_online = :lastOnline, status = :status WHERE user_id = :userId")
    suspend fun updatePresence(userId: String, isOnline: Boolean, lastOnline: OffsetDateTime?, status: String?)

    @Transaction
    suspend fun upsert(vararg users: UserEntity) = insertUser(*users)
        .withIndex()
        .filter { it.value == -1L }
        .forEach { updateUser(users[it.index]) }
}