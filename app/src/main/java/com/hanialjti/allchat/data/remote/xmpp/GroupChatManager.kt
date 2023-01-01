package com.hanialjti.allchat.data.remote.xmpp

import org.jivesoftware.smackx.bookmarks.BookmarkManager
import org.jivesoftware.smackx.muc.MultiUserChatManager

class GroupChatManager(
    private val mucManager: MultiUserChatManager,
    private val bookmarkManager: BookmarkManager
) {

}