package com.hanialjti.allchat.data.remote.xmpp.model

sealed class ConnectionConfig(
    open val chatMarkersEnabled: Boolean
)

data class XmppConnectionConfig(
    val host: String,
    val domain: String,
    val port: Int = 5222,
//    val pingConfigurations: PingConfigurations = PingConfigurations.EnablePingMessages(
//        intervalInSeconds = 30
//    ),
    override val chatMarkersEnabled: Boolean = true,
    val enableChatStateNotifications: Boolean = true,
    val useForegroundService: Boolean = true
): ConnectionConfig(chatMarkersEnabled)

sealed interface PingConfigurations {
    object DisablePingMessages: PingConfigurations
    class EnablePingMessages(val intervalInSeconds: Int): PingConfigurations
}