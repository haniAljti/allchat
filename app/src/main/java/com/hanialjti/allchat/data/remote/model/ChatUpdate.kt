package com.hanialjti.allchat.data.remote.model

import com.hanialjti.allchat.data.model.ChatState

sealed class ChatUpdate(val chatId: String)

class ChatStateUpdate(val chatState: ChatState): ChatUpdate(chatState.conversation)

class NewContactUpdate(chatId: String, val isGroupChat: Boolean): ChatUpdate(chatId)