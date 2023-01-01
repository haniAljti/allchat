package com.hanialjti.allchat.data.remote.xmpp.model

import org.jivesoftware.smack.util.XmlStringBuilder
import org.junit.Assert.*
import org.junit.Test

class BookmarkEventTest {

    @Test
    fun verifyToXMLReturnsCorrectXML() {
        val bookmark = BookmarkEvent("Bookmark Name", true, "AllChat User")
        assertEquals(
            "<conference xmlns='urn:xmpp:bookmarks:1' " +
                    "name='Bookmark Name' " +
                    "autojoin='true'>" +
                    "<nick>AllChat User</nick>" +
                    "</conference>",
            bookmark.toXML().toString()
        )
    }
}