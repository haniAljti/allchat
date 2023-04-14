package com.hanialjti.allchat.data.local.room.dao

import androidx.room.*
import com.hanialjti.allchat.data.local.room.entity.UserEntity
import com.hanialjti.allchat.data.local.room.model.ChatDetailsEntity
import com.hanialjti.allchat.data.local.room.model.UserDetailsEntity
import com.hanialjti.allchat.data.model.UserDetails
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(vararg user: UserEntity): List<Long>

    @Query("SELECT EXISTS(SELECT * FROM users WHERE id = :userId)")
    suspend fun exists(userId: String): Boolean

    @Query("UPDATE users SET is_online = :isOnline, last_online = :lastOnline, status = :status WHERE id = :userId")
    suspend fun updatePresence(userId: String, isOnline: Boolean, lastOnline: OffsetDateTime?, status: String?)

    @Transaction
    @Query(
        """
        SELECT u.id                                                          as id,
               u.status                                                      as status,
               u.is_online                                                   as isOnline,
               a.nickname                                                    as nickname,
               a.avatar_path                                                 as avatar,
               (CASE WHEN bu.blocked IS NULL THEN 0 ELSE 1 END)              as isBlocked
        FROM users u
                 LEFT JOIN entity_info a ON a.id = u.id
                 LEFT JOIN blocked_users bu ON bu.blocked = u.id
        WHERE u.id = :userId 
        """
    )
    suspend fun getUserDetails(userId: String): UserDetailsEntity?

    @Transaction
    suspend fun upsert(vararg users: UserEntity) = insertUser(*users)
        .withIndex()
        .filter { it.value == -1L }
        .forEach { updateUser(users[it.index]) }
}