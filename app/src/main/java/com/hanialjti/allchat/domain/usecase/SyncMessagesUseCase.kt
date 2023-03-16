package com.hanialjti.allchat.domain.usecase

import com.hanialjti.allchat.data.repository.AuthenticationRepository
import com.hanialjti.allchat.data.repository.IMessageRepository
import com.hanialjti.allchat.data.repository.UserRepository
import com.hanialjti.allchat.di.authenticationModule
import kotlinx.coroutines.flow.firstOrNull

class SyncMessagesUseCase(
    private val chatRepository: IMessageRepository,
    private val authenticationRepository: AuthenticationRepository
) {

    suspend operator fun invoke() {
        authenticationRepository.connectedUser.firstOrNull()?.let { user ->
            chatRepository.syncMessages(user)
        }
    }
}