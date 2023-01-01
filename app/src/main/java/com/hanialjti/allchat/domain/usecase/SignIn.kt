package com.hanialjti.allchat.domain.usecase

import com.hanialjti.allchat.data.local.datastore.UserCredentials
import com.hanialjti.allchat.data.repository.UserRepository

class SignIn(
    private val userRepository: UserRepository
) {

    suspend operator fun invoke(userCredentials: UserCredentials? = null) =
        userRepository.login(userCredentials)

}

