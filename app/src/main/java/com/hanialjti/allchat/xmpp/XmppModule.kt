package com.hanialjti.allchat.xmpp

import com.hanialjti.allchat.ConnectionManager
import org.jivesoftware.smack.ConnectionConfiguration
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration
import org.koin.dsl.module

val xmppModule = module {
    single {
        XMPPTCPConnectionConfiguration.builder()
            .setHost(get<XmppConnectionCredentials>().host)
            .setPort(get<XmppConnectionCredentials>().port)
            .setXmppDomain(get<XmppConnectionCredentials>().domain)
            .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
            .enableDefaultDebugger()
            .build()
    }
    single {
        XMPPTCPConnection(get())
    }
    single {
        XmppConnectionHelper(get())
    }
    single<ConnectionManager> {
        XmppConnectionManager(get())
    }
}