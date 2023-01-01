package com.hanialjti.allchat.domain.usecase

import com.hanialjti.allchat.data.repository.ConversationRepository
import kotlinx.coroutines.flow.first

class CreateChatRoomUseCase(
    private val conversationRepository: ConversationRepository,
    private val getGetConnectedUserUseCase: GetConnectedUserUseCase
) {
    suspend operator fun invoke() {
        val loggedInUser = getGetConnectedUserUseCase().first()

//        loggedInUser?.name?.let { conversationRepository.createChatRoom("Hello mf", loggedInUser.id) }
    }
}