package com.hanialjti.allchat.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.hanialjti.allchat.models.MessageQueryResult
import com.hanialjti.allchat.models.entity.Message
import com.hanialjti.allchat.repository.XmppChatRepository

@OptIn(ExperimentalPagingApi::class)
class MessageRemoteMediator(
    private val conversationId: String,
    private val chatRepository: XmppChatRepository
) : RemoteMediator<Int, Message>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, Message>
    ): MediatorResult {
        return when (loadType) {
            LoadType.REFRESH -> {
                MediatorResult.Success(false)
            }
            LoadType.APPEND -> {
                val mostRecentMessage = state.lastItemOrNull()

                val result = chatRepository.retrievePreviousPage(
                    mostRecentMessage,
                    conversationId,
                    state.config.pageSize
                )
                when (result) {
                    is MessageQueryResult.Success -> MediatorResult.Success(result.isEndOfList)
                    is MessageQueryResult.Error -> MediatorResult.Error(result.cause)
                }

            }
            LoadType.PREPEND -> {
                MediatorResult.Success(true)
            }
        }
    }
}