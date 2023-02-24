package com.hanialjti.allchat.data.local.remote

import com.hanialjti.allchat.data.local.room.entity.MessageEntity
import com.hanialjti.allchat.data.model.ChatState
import com.hanialjti.allchat.data.model.Marker
import com.hanialjti.allchat.data.model.MessageStatus
import com.hanialjti.allchat.data.model.MessageType
import com.hanialjti.allchat.data.remote.model.CallResult
import com.hanialjti.allchat.data.remote.model.MessagePage
import com.hanialjti.allchat.data.remote.model.RemoteMessage
import com.hanialjti.allchat.data.remote.MessageRemoteDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.time.OffsetDateTime
import java.time.ZoneOffset

class FakeRemoteMessageSource: MessageRemoteDataSource {

    override fun messageChangesStream(): Flow<RemoteMessage> {
        return emptyFlow()
    }

    override suspend fun updateMarkerForMessage(
        message: RemoteMessage,
        marker: Marker
    ): CallResult<String> {
        return CallResult.Success("fakeId")
    }

    override suspend fun sendMessage(message: MessageEntity, isMarkable: Boolean): CallResult<String> {
        return CallResult.Success("fakeId")
    }

    override suspend fun getOfflineMessages(): List<RemoteMessage> {
        TODO("Not yet implemented")
    }

    override suspend fun getPreviousPage(
        chatId: String,
        oldestMessage: RemoteMessage?,
        pageSize: Int
    ): MessagePage {
        TODO("Not yet implemented")
    }

    override suspend fun getNextPage(
        chatId: String,
        newestMessage: RemoteMessage?,
        pageSize: Int
    ): MessagePage {
        TODO("Not yet implemented")
    }

    override suspend fun syncMessages(lastMessage: RemoteMessage?, pageSize: Int): MessagePage {
        return MessagePage(
            messageList = listOf(
                RemoteMessage(
                    id = "message1",
                    body = "Hi",
                    chatId = "chat1@AllChat.conference",
                    sender = "user1@AllChat",
                    type = MessageType.Chat,
                    messageStatus = MessageStatus.Sent,
                    timestamp = OffsetDateTime.of(
                        2022,
                        11,
                        25,
                        12,
                        30,
                        0,
                        0,
                        ZoneOffset.UTC
                    )
                ),
                RemoteMessage(
                    id = "message1",
                    markers = mapOf(
                        "user1@AllChat" to Marker.Seen
                    ),
                    messageStatus = MessageStatus.Sent
                ),
                RemoteMessage(
                    id = "message2",
                    body = "Hi",
                    chatId = "chat1@AllChat.conference",
                    sender = "user1@AllChat",
                    type = MessageType.Chat,
                    messageStatus = MessageStatus.Sent,
                    timestamp = OffsetDateTime.of(
                        2022,
                        11,
                        25,
                        12,
                        30,
                        0,
                        0,
                        ZoneOffset.UTC
                    )
                )
            ),
            isComplete = true
        )
    }
}