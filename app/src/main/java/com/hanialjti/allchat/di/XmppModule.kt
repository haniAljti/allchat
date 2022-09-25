package com.hanialjti.allchat.di

import com.hanialjti.allchat.data.remote.ConnectionManager
import com.hanialjti.allchat.data.remote.xmpp.*
import org.jivesoftware.smack.ConnectionConfiguration
import org.jivesoftware.smack.ReconnectionManager
import org.jivesoftware.smack.roster.Roster
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration
import org.jivesoftware.smackx.bookmarks.BookmarkManager
import org.jivesoftware.smackx.carbons.CarbonManager
import org.jivesoftware.smackx.muc.MultiUserChatManager
import org.jivesoftware.smackx.ping.PingManager
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager
import org.koin.dsl.module

val xmppModule = module {
    single {
        XMPPTCPConnectionConfiguration.builder()
            .setHost(get<XmppConnectionConfig>().host)
            .setPort(get<XmppConnectionConfig>().port)
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
    single {
        ReconnectionManager.getInstanceFor(get()).apply {
            setReconnectionPolicy(ReconnectionManager.ReconnectionPolicy.RANDOM_INCREASING_DELAY)
            enableAutomaticReconnection()
        }
    }
    single {
        PingManager.getInstanceFor(get()).apply {
            val pingConfig = get<XmppConnectionConfig>().pingConfigurations
            pingInterval =
                if (pingConfig is PingConfigurations.EnablePingMessages) pingConfig.intervalInSeconds else -1
        }
    }
    single {
        Roster.getInstanceFor(get<XMPPTCPConnection>()).apply {
            subscriptionMode = Roster.SubscriptionMode.accept_all
        }
    }
    single {
        BookmarkManager.getBookmarkManager(get<XMPPTCPConnection>())
    }
    single {
        MultiUserChatManager.getInstanceFor(get<XMPPTCPConnection>()).apply {

        }
    }
    single {
        DeliveryReceiptManager.getInstanceFor(get<XMPPTCPConnection>()).apply {
            this.autoReceiptMode = DeliveryReceiptManager.AutoReceiptMode.always
            this.autoAddDeliveryReceiptRequests()
        }
    }
    single {
        CarbonManager.getInstanceFor(get<XMPPTCPConnection>())
    }
    single {
        GroupChatManager(get(), get())
    }
    single {
        XmppConnectionHelper(get(), get(), get(), get(), get())
    }
    single<ConnectionManager> {
        XmppConnectionManager(get(), get(), get())
    }
}