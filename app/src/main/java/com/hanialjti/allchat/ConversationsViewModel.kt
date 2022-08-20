package com.hanialjti.allchat

import androidx.lifecycle.ViewModel
import com.hanialjti.allchat.models.Conversation
import com.hanialjti.allchat.utils.currentTimestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

@HiltViewModel
class ConversationsViewModel @Inject constructor(

) : ViewModel() {

    var conversations: Flow<List<Conversation>>

    init {
        conversations = conversations("1")
    }

    fun conversations(ownerId: String): Flow<List<Conversation>> = flow {

        val conversation1 = Conversation(
            id = "",
            lastMessage = "Hello",
            isGroupChat = false,
            name = "Omar Alnaib",
            imageUrl = "",
            ownerId = "",
            members = mutableListOf(),
            unreadMessages = 3,
            lastUpdated = currentTimestamp
        )
        val conversation2 = Conversation(
            id = "",
            lastMessage = "Hello",
            isGroupChat = false,
            name = "Hani Alalajati",
            imageUrl = "",
            ownerId = "",
            members = mutableListOf(),
            unreadMessages = 100,
            lastUpdated = currentTimestamp
        )

        emit(listOf(conversation1))
        delay(1000L)
        emit(listOf(conversation1, conversation2))
    }.distinctUntilChanged()

    override fun onCleared() {
        super.onCleared()
        println("ConversationViewModel cleared!")
    }

}