package com.hanialjti.allchat.di

import com.hanialjti.allchat.data.remote.*
import com.hanialjti.allchat.data.remote.xmpp.*
import com.hanialjti.allchat.data.remote.xmpp.model.XmppConnectionConfig
import org.jivesoftware.smack.ConnectionConfiguration
import org.jivesoftware.smack.roster.rosterstore.RosterStore
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager
import org.jivesoftware.smackx.vcardtemp.VCardManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.util.*

val xmppModule = module {
    single {
        XMPPTCPConnectionConfiguration.builder()
            .setHost(get<XmppConnectionConfig>().host)
            .setPort(get<XmppConnectionConfig>().port)
            .setResource(UUID.randomUUID().toString())
//            .setSendPresence(true)
            .setXmppDomain(get<XmppConnectionConfig>().domain)
            .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
            .enableDefaultDebugger()
            .build()
    }
    single {
        RosterManager(
            get(),
            get(),
            get(named(ScopeQualifiers.Application))
        )
    }
    single {
        XMPPTCPConnection(get()).apply {
            replyTimeout = 60000
        }
    }
    single<RosterStore> { RosterDataStore(get()) }
    single {
        MucManager(
            get(),
            get(),
            get(named(ScopeQualifiers.Application)),
            get(named(DispatcherQualifiers.Io))
        )
    }
    single<FileUploader> { XmppFileUploader(get(), get(named(ScopeQualifiers.Application))) }
    single {
        ServerPingWithAlarmManager.getInstanceFor(get()).apply { isEnabled = true }
    }
    single {
        DeliveryReceiptManager.getInstanceFor(get<XMPPTCPConnection>()).apply {
            this.autoReceiptMode = DeliveryReceiptManager.AutoReceiptMode.always
            this.autoAddDeliveryReceiptRequests()
        }
    }
    single {
        VCardManager.getInstanceFor(get<XMPPTCPConnection>())
    }
    single<MessageRemoteDataSource> {
        MessageXmppDataSource(get(), get())
    }
    single<ChatRemoteDataSource> {
        ChatXmppDataSource(
            get(),
            get(),
            get()
        )
    }
    single<UserRemoteDataSource> {
        XmppUserRemoteDataSource(get(), get())
    }
    single<InfoRemoteDataSource> {
        InfoXmppDataSource(get(), get())
    }
    single<XmppClientDataSource> {
        XmppClientDataStore(androidContext())
    }
    single<ConnectionManager> {
        XmppConnectionManager(
            get(),
            get(),
            get(),
            get(qualifier = named(ScopeQualifiers.Application)),
            get(named(DispatcherQualifiers.Io)),
            get(),
            get()
        )
    }
}