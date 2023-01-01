package com.hanialjti.allchat.domain.usecase

import com.hanialjti.allchat.data.repository.ConversationRepository
import com.hanialjti.allchat.data.repository.UserRepository
import kotlinx.coroutines.flow.first

class InviteUsersToChatRoomUseCase(
    private val conversationRepository: ConversationRepository,
    private val userRepository: UserRepository
) {

    suspend operator fun invoke(conversationId: String, vararg users: String) {
        userRepository.loggedInUser.first()?.let { loggedInUser ->
            users.forEach {
                conversationRepository.inviteUserToChatRoom(
                    it, conversationId, loggedInUser.id
                )
            }
        }
    }
}