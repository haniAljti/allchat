package com.hanialjti.allchat.localdatabase

import androidx.room.Dao
import androidx.room.Query
import com.hanialjti.allchat.models.entity.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM User WHERE userId = :id")
    fun getById(id: String): Flow<User>
}