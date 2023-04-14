package com.hanialjti.allchat.common.utils

import android.os.Build
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout

object ApplicationUtils {
    var isInBackground: Boolean = true
}

inline fun <T> sdkEqualsOrUp(sdk: Int, onSdk31AndUp: () -> T): T? {
    return if (Build.VERSION.SDK_INT >= sdk) {
        onSdk31AndUp()
    } else null
}

suspend inline fun <T> suspendCoroutineWithTimeout(
    timeout: Long,
    crossinline block: (CancellableContinuation<T>) -> Unit
) = withTimeout(timeout) {
    suspendCancellableCoroutine(block = block)
}