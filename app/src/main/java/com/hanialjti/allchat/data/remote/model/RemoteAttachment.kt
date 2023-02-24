package com.hanialjti.allchat.data.remote.model

sealed interface RemoteAttachment

data class Media(
    val url: String,
    val desc: String?
): RemoteAttachment

data class Location(
    val lat: Double,
    val lng: Double
): RemoteAttachment