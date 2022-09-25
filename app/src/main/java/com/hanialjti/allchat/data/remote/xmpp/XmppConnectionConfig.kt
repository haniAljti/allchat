package com.hanialjti.allchat.data.remote.xmpp

data class XmppConnectionConfig(
    val host: String,
    val domain: String,
    val port: Int = 5222,
    val pingConfigurations: PingConfigurations = PingConfigurations.EnablePingMessages(intervalInSeconds = 30),
    val enableChatMarkers: Boolean = true,
    val enableChatStateNotifications: Boolean = true,
)

sealed interface PingConfigurations {
    object DisablePingMessages: PingConfigurations
    class EnablePingMessages(val intervalInSeconds: Int): PingConfigurations
}
