package com.hanialjti.allchat.di

import com.hanialjti.allchat.data.repository.UserRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object UserController : KoinComponent {

    private val userRepository by inject<UserRepository>()

    fun insertNewUser() {
    }
}