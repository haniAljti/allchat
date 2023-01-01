package com.hanialjti.allchat.domain.usecase

import com.hanialjti.allchat.data.remote.model.CallResult
import com.hanialjti.allchat.data.repository.ConversationRepository
import com.hanialjti.allchat.data.repository.UserRepository
import kotlinx.coroutines.flow.firstOrNull

class AddUserToContactsUseCase(
    private val conversationRepository: ConversationRepository,
    private val userRepository: UserRepository
) {

    suspend operator fun invoke(
        userId: String,
        userName: String,
        userImage: String?
    ): CallResult<String> {
        val owner = userRepository.connectedUser.firstOrNull()
            ?: return CallResult.Error("user is not signed in...")

        return conversationRepository.addContactEntry(
            userId, userName, userImage, owner.id
        )
    }
}