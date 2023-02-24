package com.hanialjti.allchat.common.utils

import timber.log.Timber

object Logger {
    @JvmStatic inline fun v(t: Throwable? = null, message: () -> String) = log { Timber.v(t, message()) }
    @JvmStatic inline fun v(t: Throwable?) = Timber.v(t)

    @JvmStatic inline fun d(t: Throwable? = null, message: () -> String) = log { Timber.d(t, message()) }
    @JvmStatic inline fun d(t: Throwable?) = Timber.d(t)

    @JvmStatic inline fun i(t: Throwable? = null, message: () -> String) = log { Timber.i(t, message()) }
    @JvmStatic inline fun i(t: Throwable?) = Timber.i(t)

    @JvmStatic inline fun w(t: Throwable? = null, message: () -> String) = log { Timber.w(t, message()) }
    @JvmStatic inline fun w(t: Throwable?) = Timber.w(t)

    @JvmStatic inline fun e(t: Throwable? = null, message: () -> String) = log { Timber.e(t, message()) }
    @JvmStatic inline fun e(t: Throwable?) = Timber.e(t)

    @JvmStatic inline fun wtf(t: Throwable? = null, message: () -> String) = log { Timber.wtf(t, message()) }
    @JvmStatic inline fun wtf(t: Throwable?) = Timber.wtf(t)
}

@PublishedApi
internal inline fun log(block: () -> Unit) {
    if (Timber.treeCount > 0) block()
}

