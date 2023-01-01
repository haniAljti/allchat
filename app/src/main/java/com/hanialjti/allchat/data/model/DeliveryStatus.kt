package com.hanialjti.allchat.data.model

enum class DeliveryStatus(val value: Int) {
    Received(-1),
    Pending(0),
    Error(1),
    Sent(2),
    Delivered(3),
    Seen(4);

    fun isMarker() = value > Sent.value
}