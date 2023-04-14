package com.hanialjti.allchat.presentation.chat

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.hanialjti.allchat.data.local.room.AllChatLocalRoomDatabase
import com.hanialjti.allchat.data.local.room.entity.MessageEntity
import com.hanialjti.allchat.data.local.room.entity.asNetworkMessage
import com.hanialjti.allchat.data.remote.MessageRemoteDataSource
import com.hanialjti.allchat.data.remote.model.RemoteGroupInvitation
import com.hanialjti.allchat.data.remote.model.RemoteMessage
import com.hanialjti.allchat.data.remote.model.asMessageEntity

@OptIn(ExperimentalPagingApi::class)
class MessageRemoteMediator(
    private val owner: String,
    private val conversationId: String,
    private val localDataStore: AllChatLocalRoomDatabase,
    private val remoteMessageRemoteDataSource: MessageRemoteDataSource
) : RemoteMediator<Int, MessageEntity>() {

    private val messageDao = localDataStore.messageDao()

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, MessageEntity>
    ): MediatorResult {

        val messagePage = when (loadType) {
            LoadType.REFRESH -> {
                val newestMessage = messageDao.getMostRecentMessageByChatId(owner, conversationId)

                remoteMessageRemoteDataSource.getNextPage(
                    conversationId,
                    newestMessage?.asNetworkMessage(),
                    Int.MAX_VALUE
                )
            }
            LoadType.APPEND -> {
//                val result = chatRepository.retrievePreviousPage(
//                    conversationId,
//                    state.config.pageSize
//                )
//                when (result) {
//                    is MessageQueryResult.Success -> MediatorResult.Success(result.isEndOfList)
//                    is MessageQueryResult.Error -> MediatorResult.Error(result.cause)
//                }
                return MediatorResult.Success(true)
            }
            LoadType.PREPEND -> {

                val oldestMessage = messageDao
                    .getFirstMessageByChatId(owner, conversationId)

                remoteMessageRemoteDataSource.getPreviousPage(
                    conversationId,
                    oldestMessage?.asNetworkMessage(),
                    state.config.pageSize
                )

            }

        }

        messagePage
            .messageList
            .map { message ->
                when (message) {
                    is RemoteMessage -> {
                        messageDao.upsertMessage(
                            message.asMessageEntity().copy(ownerId = owner)
                        )
                    }
                    is RemoteGroupInvitation -> {

                    }
                }
            }

        return MediatorResult.Success(messagePage.isComplete)
    }
}
