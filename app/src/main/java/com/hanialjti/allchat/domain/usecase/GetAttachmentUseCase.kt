package com.hanialjti.allchat.domain.usecase

import com.hanialjti.allchat.data.local.room.entity.getAttachment
import com.hanialjti.allchat.data.repository.IChatRepository

class GetAttachmentUseCase(
    private val chatRepository: IChatRepository
) {

    suspend operator fun invoke(externalMessageId: String) =
        chatRepository.getMessageByExternalId(externalMessageId)?.getAttachment()
}