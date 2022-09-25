package com.hanialjti.allchat.data.local.room

import androidx.room.TypeConverter
import com.hanialjti.allchat.data.local.room.entity.State
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {

    @TypeConverter
    fun setToString(set: Set<String>?) = set?.joinToString(",")

    @TypeConverter
    fun setFromString(set: String?): Set<String>? = set?.split(",")?.toSet()

    @TypeConverter
    fun mapToString(map: Map<String, State>) = Json.encodeToString(map)

    @TypeConverter
    fun mapFromString(map: String): Map<String, State> = Json.decodeFromString(map)

    @TypeConverter
    fun stateToString(state: State) = Json.encodeToString(state)

    @TypeConverter
    fun stateFromString(state: String): State = Json.decodeFromString(state)

    @TypeConverter
    fun listToString(list: List<String>) = list.joinToString(",")

    @TypeConverter
    fun listFromString(list: String): List<String> = list.split(",")
}