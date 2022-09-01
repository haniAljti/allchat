package com.hanialjti.allchat.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import androidx.paging.map
import com.hanialjti.allchat.R
import com.hanialjti.allchat.models.Contact
import com.hanialjti.allchat.models.ContactImage
import com.hanialjti.allchat.models.ContactInfo
import com.hanialjti.allchat.models.UiText
import com.hanialjti.allchat.repository.ConversationRepository
import com.hanialjti.allchat.utils.getDefaultDrawableRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class ConversationsViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    fun conversations(owner: String) = conversationRepository
        .conversations(owner)
        .map { conversationsAndUsers ->
            conversationsAndUsers.map {
                Contact(
                    id = it.conversation.id,
                    lastUpdated = it.conversation.lastUpdated,
                    name = it.name,
                    image = if (it.image != null) ContactImage.DynamicImage(it.image) else ContactImage.ImageRes(
                        getDefaultDrawableRes(it.conversation.isGroupChat)
                    ),
                    unreadMessages = it.conversation.unreadMessages,
                    isOnline = !it.conversation.isGroupChat && it.user?.isOnline == true,
                    isGroupChat = it.conversation.isGroupChat,
                    content = it.conversation.otherComposingUsers?.let { composing ->
                        ContactInfo.Composing(
                            text = UiText.PluralStringResource(
                                R.plurals.composing,
                                composing.count,
                                composing.userListString
                            )
                        )
                    } ?: it.conversation.lastMessage?.let { message ->
                        ContactInfo.LastMessage(
                            text = UiText.DynamicString(message),
                            read = it.conversation.unreadMessages == 0
                        )
                    }
                )
            }
        }.cachedIn(viewModelScope)

}