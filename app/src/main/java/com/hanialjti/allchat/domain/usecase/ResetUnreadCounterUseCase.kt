package com.hanialjti.allchat.domain.usecase

import com.hanialjti.allchat.data.repository.ConversationRepository

class ResetUnreadCounterUseCase(
    private val conversationRepository: ConversationRepository
) {

    suspend operator fun invoke(conversationId: String) = conversationRepository
        .resetUnreadCounter(conversationId)
}