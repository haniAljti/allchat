package com.hanialjti.allchat.data.local.room

import androidx.room.*
import com.hanialjti.allchat.data.local.room.entity.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM User WHERE userId = :id")
    fun getById(id: String): Flow<User>

    @Query("UPDATE User SET isOnline = :isOnline")
    suspend fun updateUserPresence(isOnline: Boolean)

    @Query("SELECT * FROM User WHERE userId IN (:ids)")
    suspend fun getUsers(ids: List<String>): List<User>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateUser(vararg user: User)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUser(vararg user: User): List<Long>

    @Transaction
    suspend fun upsert(vararg users: User) = insertUser(*users)
        .withIndex()
        .filter { it.value == -1L }
        .forEach { updateUser(users[it.index]) }
}