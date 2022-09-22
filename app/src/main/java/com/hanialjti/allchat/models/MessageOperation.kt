package com.hanialjti.allchat.models

import com.hanialjti.allchat.models.entity.Message
import com.hanialjti.allchat.models.entity.StatusMessage

sealed class MessageOperation(val id: String?) {
    // insert Message -> fail? -> update with UpdateMessage
    class Created(val message: Message) : MessageOperation(message.remoteId)

    // insert Message or replace old Message
    class CreatedOrUpdated(val message: Message) : MessageOperation(message.remoteId)

    // insert StatusMessage -> fail? -> update status only
    class StatusChanged(val message: StatusMessage) : MessageOperation(message.remoteId)

    // same as StatusChanged but with throwable (should be updated as Status.Error)
    // is not returned from server
    class Error(val message: StatusMessage, val cause: Throwable? = null) : MessageOperation(message.remoteId)
}

data class MessagePage(
    val messageList: List<MessageOperation> = listOf(),
    val isComplete: Boolean,
    val error: Throwable? = null
)

sealed interface MessageQueryResult {
    class Success(val isEndOfList: Boolean): MessageQueryResult
    class Error(val cause: Throwable): MessageQueryResult
}
