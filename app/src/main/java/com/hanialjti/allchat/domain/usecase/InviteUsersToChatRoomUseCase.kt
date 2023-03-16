package com.hanialjti.allchat.domain.usecase

import com.hanialjti.allchat.data.repository.AuthenticationRepository
import com.hanialjti.allchat.data.repository.ConversationRepository
import com.hanialjti.allchat.data.repository.UserRepository
import com.hanialjti.allchat.di.authenticationModule
import kotlinx.coroutines.flow.first

class InviteUsersToChatRoomUseCase(
    private val conversationRepository: ConversationRepository,
    private val authenticationRepository: AuthenticationRepository
) {

    suspend operator fun invoke(conversationId: String, vararg users: String) {
        authenticationRepository
            .loggedInUserStream
            .first()?.let { loggedInUser ->
                users.forEach {
                    conversationRepository.inviteUserToChatRoom(
                        it, conversationId, loggedInUser
                    )
                }
            }
    }
}