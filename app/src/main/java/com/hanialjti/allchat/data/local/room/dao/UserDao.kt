package com.hanialjti.allchat.data.local.room.dao

import androidx.room.*
import com.hanialjti.allchat.data.local.room.entity.UserEntity
import com.hanialjti.allchat.data.local.room.model.UserEntry
import kotlinx.coroutines.flow.Flow
import java.time.OffsetDateTime

@Dao
interface UserDao {

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun findById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE id in (SELECT id FROM chats WHERE owner = :ownerId AND is_group_chat = 0)")
    fun getByOwnerId(ownerId: String): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id in (SELECT id FROM chats WHERE owner = :ownerId AND is_group_chat = 0)")
    fun getAllByOwnerId(ownerId: String): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id IN (:ids)")
    suspend fun getUsers(ids: List<String>): List<UserEntity>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateUser(vararg user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUser(vararg user: UserEntity): List<Long>

    @Query("SELECT EXISTS(SELECT * FROM users WHERE id = :userId)")
    suspend fun exists(userId: String): Boolean

    @Query("UPDATE users SET is_online = :isOnline, last_online = :lastOnline, status = :status WHERE id = :userId")
    suspend fun updatePresence(userId: String, isOnline: Boolean, lastOnline: OffsetDateTime?, status: String?)

    @Transaction
    suspend fun upsert(vararg users: UserEntity) = insertUser(*users)
        .withIndex()
        .filter { it.value == -1L }
        .forEach { updateUser(users[it.index]) }
}