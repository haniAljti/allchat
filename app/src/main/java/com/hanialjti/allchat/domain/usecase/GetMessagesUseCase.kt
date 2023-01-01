package com.hanialjti.allchat.domain.usecase

import androidx.paging.filter
import androidx.paging.insertSeparators
import com.hanialjti.allchat.R
import com.hanialjti.allchat.common.utils.asUiDate
import com.hanialjti.allchat.data.model.MessageItem
import com.hanialjti.allchat.data.repository.IChatRepository
import com.hanialjti.allchat.data.repository.UserRepository
import com.hanialjti.allchat.presentation.conversation.UiText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class GetMessagesUseCase(
    private val chatRepository: IChatRepository,
    private val userRepository: UserRepository
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(conversationId: String) = userRepository.loggedInUser
        .flatMapLatest {
            if (it != null) {

                chatRepository.messages(conversationId, it.id)
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
                                            before.timestamp.asUiDate()
                                        )
                                    }
                                    before?.timestamp?.toLocalDate()
                                        ?.isAfter(after.timestamp.toLocalDate()) == true -> MessageItem.MessageDateSeparator(
                                        before.timestamp.asUiDate()
                                    )
                                    before != null && !before.read && after.read -> MessageItem.NewMessagesSeparator(
                                        UiText.StringResource(R.string.separator_new_messages)
                                    )
                                    else -> null
                                }
                            }
                    }
            } else emptyFlow()
        }
}