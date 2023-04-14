package com.hanialjti.allchat.data.remote.model

import com.hanialjti.allchat.data.model.Participant

sealed class ChatUpdate(val chatId: String)

class OneOnOneChatAdded(userId: String, val isGroupChat: Boolean) : ChatUpdate(userId)
class ChatUpdated(userId: String, val isGroupChat: Boolean) : ChatUpdate(userId)
class OneOnOneChatDeleted(userId: String, val isGroupChat: Boolean) : ChatUpdate(userId)
class MultiUserChatStateUpdate(val roomState: RoomState): ChatUpdate(roomState.id) {
    override fun toString(): String {
        return "MultiUserChatStateUpdate: chatId='${roomState.id}', subject='${roomState.subject}', desc='${roomState.description}', participants='${roomState.participants}'"
    }
}
class MultiUserChatDeleted(userId: String, val isGroupChat: Boolean) : ChatUpdate(userId)

class ParticipantStateUpdated(chatId: String, val participantId: String, val state: Participant.State): ChatUpdate(chatId)
