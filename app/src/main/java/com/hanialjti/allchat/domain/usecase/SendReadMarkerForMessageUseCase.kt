package com.hanialjti.allchat.domain.usecase

import com.hanialjti.allchat.data.model.MessageItem
import com.hanialjti.allchat.data.repository.IMessageRepository

class SendReadMarkerForMessageUseCase(
    private val chatRepository: IMessageRepository
) {

    suspend operator fun invoke(message: MessageItem.MessageData) =
        message.id?.let { chatRepository.sendSeenMarkerForMessage(it) }


}