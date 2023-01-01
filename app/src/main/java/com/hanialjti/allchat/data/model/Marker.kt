package com.hanialjti.allchat.data.model

enum class Marker(val value: Int) {
    Delivered(4),
    Seen(5);

    companion object {
        fun max(first: Marker, second: Marker) = if (first.value >= second.value) first else second
    }
}

fun Marker.toMessageStatus() = when (this) {
    Marker.Delivered -> MessageStatus.Delivered
    Marker.Seen -> MessageStatus.Seen
}