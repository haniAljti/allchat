package com.hanialjti.allchat.di

import com.hanialjti.allchat.data.remote.ChatRemoteDataSource
import com.hanialjti.allchat.data.remote.ConnectionManager
import com.hanialjti.allchat.data.remote.MessageRemoteDataSource
import com.hanialjti.allchat.data.remote.UserRemoteDataSource
import com.hanialjti.allchat.data.remote.xmpp.*
import com.hanialjti.allchat.data.remote.xmpp.model.PingConfigurations
import com.hanialjti.allchat.data.remote.xmpp.model.XmppConnectionConfig
import com.hanialjti.allchat.presentation.MainActivity
import org.jivesoftware.smack.ConnectionConfiguration
import org.jivesoftware.smack.roster.Roster
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration
import org.jivesoftware.smackx.bookmarks.BookmarkManager
import org.jivesoftware.smackx.ping.PingManager
import org.jivesoftware.smackx.ping.android.ServerPingWithAlarmManager
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager
import org.jivesoftware.smackx.vcardtemp.VCardManager
import org.koin.core.qualifier.named
import org.koin.dsl.module
import timber.log.Timber

val xmppModule = module {
    scope<MainActivity> {

    }
    single {
        XMPPTCPConnectionConfiguration.builder()
            .setHost(get<XmppConnectionConfig>().host)
            .setPort(get<XmppConnectionConfig>().port)
            .setSendPresence(false)
            .setXmppDomain(get<XmppConnectionConfig>().domain)
            .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
            .enableDefaultDebugger()
            .build()
    }
    single {
        XMPPTCPConnection(get()).apply {
            setUseStreamManagement(true)
            setUseStreamManagementResumption(true)
        }
    }
//    single {
//        ReconnectionManager.getInstanceFor(get()).apply {
//            setReconnectionPolicy(ReconnectionManager.ReconnectionPolicy.FIXED_DELAY)
//            enableAutomaticReconnection()
//        }
//    }
    single {
        PingManager.getInstanceFor(get()).apply {
            val pingConfig = get<XmppConnectionConfig>().pingConfigurations
            pingInterval = when (pingConfig) {
                is PingConfigurations.EnablePingMessages -> pingConfig.intervalInSeconds
                is PingConfigurations.DisablePingMessages -> -1
            }
            registerPingFailedListener {
                Timber.d("Ping failed")
            }
        }
    }
    single {
        ServerPingWithAlarmManager.getInstanceFor(get()).apply { isEnabled = true }
    }
    single {
        Roster.getInstanceFor(get<XMPPTCPConnection>()).apply {
            subscriptionMode = Roster.SubscriptionMode.accept_all
        }
    }
    single {
        BookmarkManager.getBookmarkManager(get<XMPPTCPConnection>())
    }
//    single {
//        MultiUserChatManager.getInstanceFor(get<XMPPTCPConnection>())
//    }
    single {
        DeliveryReceiptManager.getInstanceFor(get<XMPPTCPConnection>()).apply {
            this.autoReceiptMode = DeliveryReceiptManager.AutoReceiptMode.always
            this.autoAddDeliveryReceiptRequests()
        }
    }
//    single {
//        CarbonManager.getInstanceFor(get<XMPPTCPConnection>())
//    }
    single {
        VCardManager.getInstanceFor(get<XMPPTCPConnection>())
    }
    single {
        XmppUserRemoteDataSource(get())
    }
//    single {
//        GroupChatManager(get(), get())
//    }
    single<MessageRemoteDataSource> {
        XmppRemoteDataSource(get(), get())
    }
    single<ChatRemoteDataSource> {
        ChatXmppDataSource(get(), get(), get(), get())
    }
    single<UserRemoteDataSource> {
        XmppUserRemoteDataSource(get())
    }
    single<ConnectionManager> {
        XmppConnectionManager(get(), get(), get(named(DispatcherQualifiers.Io)), get())
    }
}