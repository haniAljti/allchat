package com.hanialjti.allchat.presentation.chat

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.hanialjti.allchat.data.local.room.entity.asNetworkMessage
import com.hanialjti.allchat.data.local.room.model.MessageEntry
import com.hanialjti.allchat.data.remote.model.MessageQueryResult
import com.hanialjti.allchat.data.repository.IChatRepository

@OptIn(ExperimentalPagingApi::class)
class MessageRemoteMediator(
    private val conversationId: String,
    private val chatRepository: IChatRepository
) : RemoteMediator<Int, MessageEntry>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, MessageEntry>
    ): MediatorResult {
        return when (loadType) {
            LoadType.REFRESH -> {

                val mostRecentMessage = state.firstItemOrNull()

                val result = chatRepository.retrieveNextPage(
                    conversationId,
                    mostRecentMessage?.message,
                    state.config.pageSize
                )
                when (result) {
                    is MessageQueryResult.Success -> MediatorResult.Success(true)
                    is MessageQueryResult.Error -> MediatorResult.Error(result.cause)
                }
            }
            LoadType.APPEND -> {
                val mostRecentMessage = state.lastItemOrNull()

                val result = chatRepository.retrievePreviousPage(
                    conversationId,
                    mostRecentMessage?.message,
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