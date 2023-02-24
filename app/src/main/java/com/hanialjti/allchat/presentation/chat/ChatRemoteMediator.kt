package com.hanialjti.allchat.presentation.chat

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.hanialjti.allchat.data.local.room.entity.MessageEntity
import com.hanialjti.allchat.data.remote.model.MessageQueryResult
import com.hanialjti.allchat.data.repository.IMessageRepository

@OptIn(ExperimentalPagingApi::class)
class MessageRemoteMediator(
    private val conversationId: String,
    private val chatRepository: IMessageRepository
) : RemoteMediator<Int, MessageEntity>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, MessageEntity>
    ): MediatorResult {
        return when (loadType) {
            LoadType.REFRESH -> {
                return MediatorResult.Success(false)
            }
            LoadType.APPEND -> {
                val result = chatRepository.retrievePreviousPage(
                    conversationId,
                    state.config.pageSize
                )
                when (result) {
                    is MessageQueryResult.Success -> MediatorResult.Success(result.isEndOfList)
                    is MessageQueryResult.Error -> MediatorResult.Error(result.cause)
                }
            }
            LoadType.PREPEND -> {
                val result = chatRepository.retrieveNextPage(
                    conversationId,
                    state.config.pageSize
                )
                when (result) {
                    is MessageQueryResult.Success -> MediatorResult.Success(result.isEndOfList)
                    is MessageQueryResult.Error -> MediatorResult.Error(result.cause)
                }
            }
        }
    }
}