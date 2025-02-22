package com.hanialjti.allchat

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import com.hanialjti.allchat.data.remote.ConnectionType
import com.hanialjti.allchat.data.remote.xmpp.model.XmppConnectionConfig
import com.hanialjti.allchat.di.AllChat
import timber.log.Timber
import timber.log.Timber.DebugTree
import timber.log.Timber.Forest.plant

class AllChatApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AllChat.initialize(
            this,
            ConnectionType.Xmpp(
                XmppConnectionConfig(
                    host = "192.168.0.205",
                    domain = "hanis-laptop",
                    port = 5222
                )
            )
        )



        if (Timber.forest().isEmpty()) {
            plant(DebugTree())
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        AllChat.stop()
    }


}

///** A tree which logs important information for crash reporting. */
//private static class CrashReportingTree extends Timber.Tree {
//    @Override protected void log(int priority, String tag, @NonNull String message, Throwable t) {
//        if (priority == Log.VERBOSE || priority == Log.DEBUG) {
//            return;
//        }
//
//        FakeCrashLibrary.log(priority, tag, message);
//
//        if (t != null) {
//            if (priority == Log.ERROR) {
//                .logError(t);
//            } else if (priority == Log.WARN) {
//                FakeCrashLibrary.logWarning(t);
//            }
//        }
//    }
//}
//}