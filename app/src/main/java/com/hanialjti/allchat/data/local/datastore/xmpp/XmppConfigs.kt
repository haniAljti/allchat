package com.hanialjti.allchat.data.local.datastore.xmpp

import androidx.datastore.core.Serializer
import com.hanialjti.allchat.data.remote.model.RoomState
import com.hanialjti.allchat.data.remote.xmpp.asJid
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.jivesoftware.smack.roster.packet.RosterPacket
import org.jivesoftware.smack.roster.packet.RosterPacket.Item
import java.io.InputStream
import java.io.OutputStream

@Serializable
data class XmppConfigs(
    val nickname: String? = null,
    val chatRooms: Set<RoomState> = setOf(),
    val rosterItems: Set<RosterItem> = setOf(),
    val rosterVersion: String = ""
)

@Serializable
data class RosterItem(
    val jid: String,
    val name: String,
    val subscriptionPending: Boolean
) {
    suspend fun asSmackRosterItem() = Item(jid.asJid(), name, subscriptionPending)
}

fun Item.asRosterItem() = RosterItem(jid.toString(), name, isSubscriptionPending)

class XmppConfigsSerializer : Serializer<XmppConfigs> {

    override val defaultValue: XmppConfigs
        get() = XmppConfigs()

    override suspend fun readFrom(input: InputStream): XmppConfigs {
        return try {
            Json.decodeFromString(
                deserializer = XmppConfigs.serializer(),
                string = input.readBytes().decodeToString()
            )
        } catch (e: SerializationException) {
            e.printStackTrace()
            defaultValue
        }
    }

    override suspend fun writeTo(t: XmppConfigs, output: OutputStream) {
        output.write(
            Json.encodeToString(
                serializer = XmppConfigs.serializer(),
                value = t
            ).encodeToByteArray()
        )
    }

}