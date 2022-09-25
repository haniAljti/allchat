package com.hanialjti.allchat.data.remote.xmpp

import com.hanialjti.allchat.data.remote.CallResult
import org.jivesoftware.smack.SmackException
import org.jivesoftware.smack.XMPPException
import org.jivesoftware.smackx.bookmarks.BookmarkManager
import org.jivesoftware.smackx.muc.MultiUserChatManager
import org.jxmpp.jid.impl.JidCreate
import org.jxmpp.jid.parts.Resourcepart

class GroupChatManager(
    private val mucManager: MultiUserChatManager,
    private val bookmarkManager: BookmarkManager
) {

    suspend fun createChatRoom(
        name: String,
        chatRoomAddress: String,
        owner: String
    ): CallResult<String?> {
        return try {
            val muc = mucManager.getMultiUserChat(JidCreate.entityBareFrom(chatRoomAddress.asJid()))
            muc.create(Resourcepart.from("Group")).makeInstant()
            bookmarkConference(chatRoomAddress, name, owner)
            CallResult.Success(null)
        } catch (e: Exception) {
            CallResult.Error("An error occurred while creating chat room", e)
        }
    }

    suspend fun addGroupChatToContacts(
        conversationId: String,
        conversationName: String,
        myNickName: String
    ): CallResult<String?> {
        return try {
            bookmarkConference(conversationId, conversationName, myNickName)
            CallResult.Success(null)
        } catch (e: Exception) {
            CallResult.Error("An error occurred while creating chat room", e)
        }
    }

    private suspend fun bookmarkConference(
        conversationId: String,
        conversationName: String,
        myNickName: String
    ) {
        bookmarkManager
            .addBookmarkedConference(
                conversationName,
                conversationId.asJid().asEntityBareJidIfPossible(),
                true,
                Resourcepart.from(myNickName),
                null
            )
    }

}