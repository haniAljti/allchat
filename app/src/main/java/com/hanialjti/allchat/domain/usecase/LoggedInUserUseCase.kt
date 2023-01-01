package com.hanialjti.allchat.domain.usecase

import com.hanialjti.allchat.data.repository.UserRepository
import kotlinx.coroutines.flow.combine

class LoggedInUserUseCase(
    private val userRepository: UserRepository
) {
    operator fun invoke() = userRepository.loggedInUser
}