package com.hanialjti.allchat.xmpp

import com.hanialjti.allchat.ConnectionManager
import org.jivesoftware.smack.ConnectionConfiguration
import org.jivesoftware.smack.ReconnectionManager
import org.jivesoftware.smack.roster.Roster
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration
import org.jivesoftware.smackx.carbons.CarbonManager
import org.jivesoftware.smackx.ping.PingManager
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
        CarbonManager.getInstanceFor(get<XMPPTCPConnection>())
    }
    single {
        XmppConnectionHelper(get(), get(), get())
    }
    single<ConnectionManager> {
        XmppConnectionManager(get(), get(), get())
    }
}