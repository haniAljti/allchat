package com.hanialjti.allchat.data.repository

import com.hanialjti.allchat.data.local.room.AllChatLocalRoomDatabase
import com.hanialjti.allchat.data.local.room.entity.User

class UserRepository constructor(
    localDb: AllChatLocalRoomDatabase
) {

    private val userDao = localDb.userDao()

    fun user(userId: String) = userDao.getById(userId)

    suspend fun insertUser(user: User) = userDao.insertUser(user)

    suspend fun getUsers(userIds: List<String>) = userDao.getUsers(userIds)
}