package com.hanialjti.allchat

import android.app.Application
import com.hanialjti.allchat.xmpp.XmppConnectionConfig
import org.jivesoftware.smack.android.AndroidSmackInitializer
import timber.log.Timber
import timber.log.Timber.DebugTree
import timber.log.Timber.Forest.plant

class AllChatApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        AllChat.initialize(
            this,
            ConnectionType.Xmpp(
                XmppConnectionConfig(
                    host = "192.168.0.42",
                    domain = "localhost",
                    port = 5222
                )
            )
        )

        AndroidSmackInitializer.initialize(this)


        if (Timber.forest().isEmpty()) {
            plant(DebugTree())
        }
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