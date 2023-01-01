package com.hanialjti.allchat.data.model

import com.hanialjti.allchat.presentation.chat.Attachment

data class Location(
    val id: Long? = 0,
    val lat: Double,
    val lng: Double
)

fun Location.asAttachment() = Attachment.Location(
    lat = lat,
    lng = lng
)