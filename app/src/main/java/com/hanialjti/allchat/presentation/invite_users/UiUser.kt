package com.hanialjti.allchat.presentation.invite_users

import com.hanialjti.allchat.presentation.conversation.ContactImage

data class UiUser(
    val id: String?,
    val name: String?,
    val image: ContactImage?,
    val isOnline: Boolean,
    val lastOnline: Long?
)
