package com.hanialjti.allchat.domain.usecase

import com.hanialjti.allchat.data.repository.IChatRepository
import com.hanialjti.allchat.data.repository.UserRepository
import kotlinx.coroutines.flow.firstOrNull

class SyncMessagesUseCase(
    private val chatRepository: IChatRepository,
    private val userRepository: UserRepository
) {

    suspend operator fun invoke() {
        userRepository.connectedUser.firstOrNull()?.let { user ->
            chatRepository.syncMessages(user)
        }
    }
}