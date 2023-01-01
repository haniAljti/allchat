package com.hanialjti.allchat.data.local.remote

import com.hanialjti.allchat.data.local.room.entity.MessageEntity
import com.hanialjti.allchat.data.model.ChatState
import com.hanialjti.allchat.data.model.Marker
import com.hanialjti.allchat.data.model.MessageStatus
import com.hanialjti.allchat.data.model.MessageType
import com.hanialjti.allchat.data.remote.model.CallResult
import com.hanialjti.allchat.data.remote.model.MessagePage
import com.hanialjti.allchat.data.remote.NetworkMessage
import com.hanialjti.allchat.data.remote.MessageRemoteDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.time.OffsetDateTime
import java.time.ZoneOffset

class FakeRemoteMessageSource: MessageRemoteDataSource {
    override suspend fun updateMyChatState(chatState: ChatState) {

    }

    override fun listenForMessageChanges(): Flow<NetworkMessage> {
        return emptyFlow()
    }

    override suspend fun updateMarkerForMessage(
        message: NetworkMessage,
        marker: Marker
    ): CallResult<String> {
        return CallResult.Success("fakeId")
    }

    override suspend fun sendMessage(message: MessageEntity): CallResult<String> {
        return CallResult.Success("fakeId")
    }

    override suspend fun getPreviousPage(
        beforeTimestamp: OffsetDateTime?,
        conversationId: String?,
        pageSize: Int
    ): MessagePage {
        TODO("Not yet implemented")
    }

    override suspend fun syncMessages(afterMessage: MessageEntity?, pageSize: Int): MessagePage {
        return MessagePage(
            messageList = listOf(
                NetworkMessage(
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
                NetworkMessage(
                    id = "message1",
                    markers = mapOf(
                        "user1@AllChat" to Marker.Seen
                    ),
                    messageStatus = MessageStatus.Sent
                ),
                NetworkMessage(
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