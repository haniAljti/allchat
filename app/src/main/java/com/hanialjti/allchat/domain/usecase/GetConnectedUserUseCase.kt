package com.hanialjti.allchat.domain.usecase

import com.hanialjti.allchat.data.repository.AuthenticationRepository
import com.hanialjti.allchat.data.repository.UserRepository
import kotlinx.coroutines.flow.combine

class GetConnectedUserUseCase(
    private val authenticationRepository: AuthenticationRepository
) {
    operator fun invoke() = authenticationRepository.connectedUser
}