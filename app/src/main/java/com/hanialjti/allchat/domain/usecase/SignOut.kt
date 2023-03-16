package com.hanialjti.allchat.domain.usecase

import com.hanialjti.allchat.data.repository.AuthenticationRepository
import com.hanialjti.allchat.data.repository.UserRepository

class SignOut(
    private val authenticationRepository: AuthenticationRepository
) {
    suspend operator fun invoke() = authenticationRepository.disconnect()

}