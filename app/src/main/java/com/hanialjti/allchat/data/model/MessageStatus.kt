package com.hanialjti.allchat.data.model

enum class MessageStatus(val value: Int) {
    Pending(0),
    Error(1),
    Sending(2),
    Sent(3),
    Delivered(4),
    Seen(5);

    companion object {
        fun max(first: MessageStatus, second: MessageStatus) = if (first.value >= second.value) first else second
    }
}