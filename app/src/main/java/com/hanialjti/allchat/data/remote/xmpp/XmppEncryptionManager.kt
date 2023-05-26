package com.hanialjti.allchat.data.remote.xmpp

import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smackx.omemo.OmemoConfiguration
import org.jivesoftware.smackx.omemo.OmemoInitializer
import org.jivesoftware.smackx.omemo.OmemoManager
import org.jivesoftware.smackx.omemo.signal.SignalOmemoService

class XmppEncryptionManager(
    private val connection: XMPPTCPConnection
) {

    init {
        SignalOmemoService.acknowledgeLicense()
        SignalOmemoService.setup()
        OmemoManager.getInstanceFor(connection)
    }
}