package com.hanialjti.allchat.domain.usecase

import com.hanialjti.allchat.data.repository.UserRepository
import com.hanialjti.allchat.data.repository.ChatRepository
import com.hanialjti.allchat.data.repository.IChatRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest

class GetMostRecentMessageUseCase(
    private val chatRepository: IChatRepository,
    private val userRepository: UserRepository
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(conversationId: String) = userRepository
            .loggedInUser
            .flatMapLatest {
                it?.let { user ->
                    chatRepository.observeLastMessageNotSentByOwner(user.id, conversationId)
                } ?: emptyFlow()
            }
        .filterNotNull()
}