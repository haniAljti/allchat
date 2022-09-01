package com.hanialjti.allchat.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.hanialjti.allchat.xmpp.XmppConnectionHelper
import com.hanialjti.allchat.localdatabase.AllChatLocalRoomDatabase
import com.hanialjti.allchat.models.entity.Message
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    localDb: AllChatLocalRoomDatabase
) {

    private val messageDao = localDb.messageDao()
    private val conversationDao = localDb.conversationDao()
    private val userDao = localDb.userDao()

    fun messages(conversation: String) = Pager(
        config = PagingConfig(
            pageSize = 20
        ),
        pagingSourceFactory = { messageDao.getMessagesByConversation(conversation) }
    ).flow

    suspend fun sendMessage(message: Message) {
        messageDao.insert(message)
        message.body?.let { XmppConnectionHelper.sendMessage(it) }
    }

    fun getMessageById(messageId: String) = messageDao.getMessageById(messageId)

    suspend fun saveMessageContentUri(messageId: String, contentUri: String) =
        messageDao.saveContentUri(messageId, contentUri)


//    fun user(conversationId: String) = flow {
//
//        val conversation = conversationDao.getById(conversationId)
//
//        if (conversation.isGroupChat) {
//            emit(
//                User(
//                    name = conversation.name,
//                    image = conversation.imageUrl
//                )
//            )
//        }
//
//        else {
//            val user = conversation.to?.let { userDao.getById(it) }
//            user?.let { emitAll(user) }
//        }
//    }

}