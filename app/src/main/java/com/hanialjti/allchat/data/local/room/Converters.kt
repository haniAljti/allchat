package com.hanialjti.allchat.data.local.room

import androidx.room.TypeConverter
import com.hanialjti.allchat.data.local.room.entity.*
import com.hanialjti.allchat.data.model.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class Converters {

    private val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    @TypeConverter
    fun attachmentTypeToString(attachmentType: Attachment.Type) = attachmentType.value

    @TypeConverter
    fun attachmentTypeFromString(value: Int): Attachment.Type = Attachment.Type.fromValue(value)

    @TypeConverter
    fun attachmentToString(attachment: Attachment?) = attachment?.let { Json.encodeToString(AttachmentSerializer, attachment) }

    @TypeConverter
    fun attachmentFromString(value: String?): Attachment? = value?.let { Json.decodeFromString(AttachmentSerializer, value) }

    @TypeConverter
    fun messageSummaryToString(message: MessageSummary?) = message?.let { Json.encodeToString(message) }

    @TypeConverter
    fun messageSummaryFromString(value: String?): MessageSummary? = value?.let {
        Json.decodeFromString(it)
    }

    @TypeConverter
    fun setToString(set: Set<String>?) = set?.joinToString(",")

    @TypeConverter
    fun setFromString(set: String?): Set<String>? = set?.split(",")?.toSet()

    @TypeConverter
    fun statesToString(map: Map<String, Participant.State>) = Json.encodeToString(map)

    @TypeConverter
    fun statesFromString(map: String): Map<String, Participant.State> = Json.decodeFromString(map)

    @TypeConverter
    fun markersToString(map: Map<String, Marker>?) = Json.encodeToString(map ?: mutableMapOf())

    @TypeConverter
    fun markersFromString(map: String?): Map<String, Marker> = Json.decodeFromString(map ?: "{}")

    @TypeConverter
    fun stateToString(state: Participant.State) = Json.encodeToString(state)

    @TypeConverter
    fun offsetDateTimeToString(dateTime: OffsetDateTime?) = dateTime?.format(formatter)

    @TypeConverter
    fun offsetDateTimeFromString(dateTime: String?) = dateTime?.let { return@let formatter.parse(it, OffsetDateTime::from) }

    @TypeConverter
    fun stateFromString(state: String): Participant.State = Json.decodeFromString(state)

    @TypeConverter
    fun listToString(list: List<String>) = list.joinToString(",")

    @TypeConverter
    fun listFromString(list: String): List<String> = list.split(",")

    @TypeConverter
    fun roleListToString(list: List<Role>) = Json.encodeToString(list)

    @TypeConverter
    fun roleListFromString(list: String): List<Role> = Json.decodeFromString(list)

    @TypeConverter
    fun messageMarkerMapToString(markers: Map<String, MessageMarker>) = Json.encodeToString(markers)

    @TypeConverter
    fun messageMarkerMapFromString(markersMap: String): Map<String, MessageMarker> = Json.decodeFromString(markersMap)

    @TypeConverter
    fun fromStatus(status: MessageStatus?) = status?.value

    @TypeConverter
    fun toStatus(statusValue: Int?) = when (statusValue) {
        0 -> MessageStatus.Pending
        1 -> MessageStatus.Error
        2 -> MessageStatus.Sending
        3 -> MessageStatus.Sent
        4 -> MessageStatus.Delivered
        else -> MessageStatus.Seen
    }

    @TypeConverter
    fun fromRole(role: Role?) = role?.value

    @TypeConverter
    fun toRole(roleValue: Int?) = when (roleValue) {
        0 -> Role.Participant
        else -> Role.Admin
    }

    @TypeConverter
    fun fromMessageType(messageType: MessageType?) = messageType?.ordinal

    @TypeConverter
    fun toMessageType(messageTypeValue: Int?) = when (messageTypeValue) {
        0 -> MessageType.Chat
        else -> MessageType.GroupChat
    }

    @TypeConverter
    fun fromMarker(marker: Marker) = marker.value

    @TypeConverter
    fun toMarker(markerValue: Int) = when (markerValue) {
        4 -> Marker.Delivered
        else -> Marker.Seen
    }
}