package com.hanialjti.allchat.common.utils

import android.os.Build

object ApplicationUtils {
    var isInBackground: Boolean = true
}

inline fun <T> sdkEqualsOrUp(sdk: Int, onSdk31AndUp: () -> T): T? {
    return if (Build.VERSION.SDK_INT >= sdk) {
        onSdk31AndUp()
    } else null
}