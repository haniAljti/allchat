package com.hanialjti.allchat.localdatabase

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun setToString(set: Set<String>) = set.joinToString(",")

    @TypeConverter
    fun setFromString(set: String): Set<String> = set.split(",").toSet()

    @TypeConverter
    fun listToString(list: List<String>) = list.joinToString(",")

    @TypeConverter
    fun listFromString(list: String): List<String> = list.split(",")
}