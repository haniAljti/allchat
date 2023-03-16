package com.hanialjti.allchat.data.local.datastore.xmpp

import androidx.datastore.core.Serializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

@Serializable
data class XmppConfigs(
    val nickname: String? = null,
    val chatRooms: Set<String> = setOf()
)

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