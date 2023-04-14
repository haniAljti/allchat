package com.hanialjti.allchat.data.remote.xmpp.model

import com.hanialjti.allchat.data.remote.model.OneOnOneChatAdded
import com.hanialjti.allchat.data.remote.model.OneOnOneChatDeleted
import com.hanialjti.allchat.data.remote.model.ChatUpdated
import com.hanialjti.allchat.data.remote.model.PresenceUpdate
import java.time.OffsetDateTime

sealed class RosterUpdate(val userId: String)

class ItemAdded(userId: String) : RosterUpdate(userId) {
    fun toChatUpdate() = OneOnOneChatAdded(userId, false)
}
class ItemUpdated(userId: String) : RosterUpdate(userId) {
    fun toChatUpdate() = ChatUpdated(userId, false)
}
class ItemDeleted(userId: String) : RosterUpdate(userId) {
    fun toChatUpdate() = OneOnOneChatDeleted(userId, false)
}

class PresenceUpdated(
    userId: String,
    val isOnline: Boolean,
    val status: String?,
    val lastOnline: OffsetDateTime?
) : RosterUpdate(userId) {
    fun toUserUpdate() = PresenceUpdate(userId, isOnline, status, lastOnline)
}

class PresenceSubscriptionApproved(userId: String) : RosterUpdate(userId)
class PresenceSubscriptionDenied(userId: String) : RosterUpdate(userId)
class PresenceSubscriptionArrived(userId: String) : RosterUpdate(userId)
