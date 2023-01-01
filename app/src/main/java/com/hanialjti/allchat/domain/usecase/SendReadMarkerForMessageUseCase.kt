package com.hanialjti.allchat.domain.usecase

import com.hanialjti.allchat.data.model.MessageItem
import com.hanialjti.allchat.data.repository.IChatRepository

class SendReadMarkerForMessageUseCase(
    private val chatRepository: IChatRepository
) {

    suspend operator fun invoke(message: MessageItem.MessageData) =
        message.id?.let { chatRepository.sendSeenMarkerForMessage(it) }


}