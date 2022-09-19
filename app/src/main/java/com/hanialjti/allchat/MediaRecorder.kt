package com.hanialjti.allchat

import android.os.Build

inline fun <T> sdk26AndUp(onSdk26AndUp: () -> T): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        onSdk26AndUp()
    } else null
}