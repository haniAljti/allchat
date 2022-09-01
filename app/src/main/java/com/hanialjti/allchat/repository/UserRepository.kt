package com.hanialjti.allchat.repository

import com.hanialjti.allchat.localdatabase.AllChatLocalRoomDatabase
import com.hanialjti.allchat.models.entity.User
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    localDb: AllChatLocalRoomDatabase
) {

    private val userDao = localDb.userDao()

    fun user(userId: String) = userDao.getById(userId)

}