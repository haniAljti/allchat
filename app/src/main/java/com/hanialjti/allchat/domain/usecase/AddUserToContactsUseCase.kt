package com.hanialjti.allchat.domain.usecase

import com.hanialjti.allchat.data.remote.model.CallResult
import com.hanialjti.allchat.data.repository.ConversationRepository

class AddUserToContactsUseCase(
    private val conversationRepository: ConversationRepository
) {

    suspend operator fun invoke(
        userId: String
    ): CallResult<String> {
        return conversationRepository.addUserToContactList(userId)
    }
}