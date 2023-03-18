package com.hanialjti.allchat.data.local.room.model

import androidx.room.Embedded
import com.hanialjti.allchat.data.local.room.entity.ChatEntity
import com.hanialjti.allchat.data.local.room.entity.UserEntity
import com.hanialjti.allchat.data.local.room.entity.asUser
import com.hanialjti.allchat.data.model.ChatInfo
import com.hanialjti.allchat.data.model.Participant
import com.hanialjti.allchat.data.model.Role
import com.hanialjti.allchat.presentation.conversation.ContactImage

data class Chat(
    @Embedded("chat_")
    val chat: ChatEntity,
    @Embedded("user_")
    val user: UserEntity?,
    val avatar: String?,
    val nickname: String?
)

fun Chat.asChatInfo() = ChatInfo(
    id = chat.id,
    isGroupChat = chat.isGroupChat,
    name = nickname,
    image = avatar?.let { ContactImage.DynamicImage(it) }
        ?: ContactImage.DefaultProfileImage(chat.isGroupChat),
    isOnline = user?.isOnline ?: false,
    description = null,
    participants = chat.participants
        .mapNotNull {
            user?.let {
                Participant(
                    it.asUser(),
                    Participant.State.Active,
                    Role.Participant
                )
            }
        }
        .toSet()
)
