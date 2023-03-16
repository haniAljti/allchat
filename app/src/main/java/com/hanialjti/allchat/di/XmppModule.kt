package com.hanialjti.allchat.di

import com.hanialjti.allchat.data.remote.*
import com.hanialjti.allchat.data.remote.xmpp.*
import com.hanialjti.allchat.data.remote.xmpp.model.XmppConnectionConfig
import org.jivesoftware.smack.ConnectionConfiguration
import org.jivesoftware.smack.roster.Roster
import org.jivesoftware.smack.roster.rosterstore.DirectoryRosterStore
import org.jivesoftware.smack.roster.rosterstore.RosterStore
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager
import org.jivesoftware.smackx.vcardtemp.VCardManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.io.File
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
        XMPPTCPConnection(get()).apply {
            replyTimeout = 60000
        }
    }
    single<RosterStore> {
        androidContext().let {
            val rosterStoreDir = File(it.cacheDir, "roster")
            val rosterStore = if (rosterStoreDir.exists() && rosterStoreDir.isDirectory) {
                DirectoryRosterStore.open(rosterStoreDir)
            } else {
                rosterStoreDir.mkdirs()
                DirectoryRosterStore.init(rosterStoreDir)
            }
            get<Roster>().setRosterStore(rosterStore)
            rosterStore
        }
    }
    single {
        MucManager(
            get(),
            get(),
            get(named(DispatcherQualifiers.Io))
        )
    }
//    single {
//        PingManager.getInstanceFor(get()).apply {
//            val pingConfig = get<XmppConnectionConfig>().pingConfigurations
//            pingInterval = when (pingConfig) {
//                is PingConfigurations.EnablePingMessages -> pingConfig.intervalInSeconds
//                is PingConfigurations.DisablePingMessages -> -1
//            }
//            registerPingFailedListener {
//                Timber.d("Ping failed")
//            }
//        }
//    }
    single { FileXmppDataSource(get(), get(named(ScopeQualifiers.Application))) }
    single {
        ServerPingWithAlarmManager.getInstanceFor(get()).apply { isEnabled = true }
    }
    single {
        Roster.getInstanceFor(get<XMPPTCPConnection>()).apply {
            subscriptionMode = Roster.SubscriptionMode.accept_all
        }
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
    single {
        XmppUserRemoteDataSource(get())
    }
    single<MessageRemoteDataSource> {
        XmppRemoteDataSource(get(), get())
    }
    single<ChatRemoteDataSource> {
        ChatXmppDataSource(get(), get(),  get(named(DispatcherQualifiers.Io)), get(named(ScopeQualifiers.Application)), get())
    }
    single<UserRemoteDataSource> {
        XmppUserRemoteDataSource(get())
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