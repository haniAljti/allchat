package com.hanialjti.allchat.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.hanialjti.allchat.R
import com.hanialjti.allchat.localdatabase.AllChatLocalRoomDatabase
import com.hanialjti.allchat.models.Contact
import com.hanialjti.allchat.models.ContactInfo
import com.hanialjti.allchat.models.UiText
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepository @Inject constructor(
    localDb: AllChatLocalRoomDatabase
) {

    private val conversationDao = localDb.conversationDao()

    fun conversations(owner: String) = Pager(
        config = PagingConfig(
            pageSize = 20,
            enablePlaceholders = true
        ),
        pagingSourceFactory = {
            conversationDao.getConversationsAndUsers(owner)
        }
    ).flow

    fun conversation(conversationId: String) = conversationDao.getConversationAndUser(conversationId)
}