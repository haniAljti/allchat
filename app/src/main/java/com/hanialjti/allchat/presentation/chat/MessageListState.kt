package com.hanialjti.allchat.presentation.chat

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.delay

class MessageListState(

) {

    var flashingMessageId: MutableState<String?> = mutableStateOf(null)

    suspend fun animateFlashingMessage(messageId: String) {
        flashingMessageId.value = messageId
        delay(2000)
        flashingMessageId.value = null
    }
}