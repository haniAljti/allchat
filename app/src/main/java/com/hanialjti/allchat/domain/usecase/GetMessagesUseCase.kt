package com.hanialjti.allchat.domain.usecase

import androidx.paging.filter
import androidx.paging.insertSeparators
import com.hanialjti.allchat.R
import com.hanialjti.allchat.common.utils.asUiDate
import com.hanialjti.allchat.data.model.MessageItem
import com.hanialjti.allchat.data.repository.IChatRepository
import com.hanialjti.allchat.presentation.conversation.UiText
import kotlinx.coroutines.flow.map
import kotlinx.datetime.toJavaLocalDateTime

class GetMessagesUseCase(private val chatRepository: IChatRepository) {

    operator fun invoke(conversationId: String) =

        chatRepository.fetchMessagesFor(conversationId)
            .map { messages ->
                messages
                    .filter { message ->
                        message.body != null || message.attachment != null
                    }
                    .insertSeparators { before, after ->
                        when {
                            after == null && before == null -> null
                            after == null -> before?.let {
                                MessageItem.MessageDateSeparator(
                                    before.timestamp.toJavaLocalDateTime().asUiDate()
                                )
                            }
                            before?.timestamp?.toJavaLocalDateTime()?.toLocalDate()
                                ?.isAfter(after.timestamp.toJavaLocalDateTime().toLocalDate()) == true -> MessageItem.MessageDateSeparator(
                                before.timestamp.toJavaLocalDateTime().asUiDate()
                            )
                            before != null && !before.read && after.read -> MessageItem.NewMessagesSeparator(
                                UiText.StringResource(R.string.separator_new_messages)
                            )
                            else -> null
                        }
                    }
            }
}