package com.hanialjti.allchat.domain.usecase

import com.hanialjti.allchat.data.repository.ConversationRepository

class SyncChatsUseCase(
    private val conversationRepository: ConversationRepository
) {

    suspend operator fun invoke() {
        conversationRepository.syncChats()
    }

}