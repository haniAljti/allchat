package com.hanialjti.allchat.models.entity

import androidx.room.TypeConverter

class MessageStatusConverter {

    @TypeConverter
    fun fromStatus(status: Status) = status.ordinal

    @TypeConverter
    fun toStatus(statusValue: Int) = when (statusValue) {
        0 -> Status.Error
        1 -> Status.Pending
        2 -> Status.Sent
        3 -> Status.Acknowledged
        4 -> Status.Received
        else -> Status.Seen
    }
}