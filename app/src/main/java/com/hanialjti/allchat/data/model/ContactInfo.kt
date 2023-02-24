package com.hanialjti.allchat.data.model

import com.hanialjti.allchat.presentation.chat.defaultName
import com.hanialjti.allchat.presentation.conversation.ContactImage
import com.hanialjti.allchat.presentation.conversation.UiText

data class ContactInfo(
    val name: String = defaultName,
    val image: ContactImage? = null,
    val content: UiText? = null
)