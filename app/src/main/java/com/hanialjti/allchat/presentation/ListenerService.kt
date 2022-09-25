package com.hanialjti.allchat.presentation

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

class ListenerService: Service() {

    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder = binder

    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): ListenerService = this@ListenerService
    }
}