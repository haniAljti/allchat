package com.hanialjti.allchat

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.jivesoftware.smack.AbstractXMPPConnection
import org.jivesoftware.smack.android.AndroidSmackInitializer
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration

@HiltAndroidApp
class AllChatApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidSmackInitializer.initialize(this)
    }
}