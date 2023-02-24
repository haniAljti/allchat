package com.hanialjti.allchat.data.remote.xmpp.model

import org.junit.Assert.*
import org.junit.Test

class MucBookmarkTest {

    @Test
    fun verifyToXMLReturnsCorrectXML() {
        val bookmark = MucBookmark("Bookmark Name", true, "AllChat User")
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