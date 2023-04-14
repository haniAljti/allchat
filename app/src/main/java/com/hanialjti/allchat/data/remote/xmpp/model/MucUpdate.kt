package com.hanialjti.allchat.data.remote.xmpp.model

import com.hanialjti.allchat.data.remote.model.*

sealed class MucUpdate(val chatId: String)

class MucStateUpdate(val roomState: RoomState): MucUpdate(roomState.id) {
    fun toChatUpdate() = MultiUserChatStateUpdate(roomState)
}

class MucMessageUpdate(val message: RemoteMessage): MucUpdate(message.chatId)