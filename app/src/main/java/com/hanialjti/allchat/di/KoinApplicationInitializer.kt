package com.hanialjti.allchat.di

import android.content.Context
import androidx.startup.Initializer
import com.hanialjti.allchat.data.remote.ConnectionType
import com.hanialjti.allchat.data.remote.xmpp.model.XmppConnectionConfig
import org.koin.core.KoinApplication

class KoinApplicationInitializer: Initializer<KoinApplication> {
    override fun create(context: Context): KoinApplication {
        return AllChat.initialize(
            context = context,
            ConnectionType.Xmpp(
                XmppConnectionConfig(
                    host = "192.168.0.42",
                    domain = "localhost",
                    port = 5222
                )
            )
        )
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> {
        return mutableListOf()
    }
}