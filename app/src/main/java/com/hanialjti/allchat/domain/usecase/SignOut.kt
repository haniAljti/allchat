package com.hanialjti.allchat.domain.usecase

import com.hanialjti.allchat.data.repository.UserRepository

class SignOut(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke() = userRepository.disconnect()
}