package com.hanialjti.allchat

import android.app.Application
import com.hanialjti.allchat.xmpp.DataExtensionElement
import com.hanialjti.allchat.xmpp.DataItemExtensionProvider
import org.jivesoftware.smack.android.AndroidSmackInitializer
import org.jivesoftware.smack.provider.ProviderManager
import timber.log.Timber
import timber.log.Timber.DebugTree
import timber.log.Timber.Forest.plant

class AllChatApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidSmackInitializer.initialize(this)
        ProviderManager.addExtensionProvider(DataExtensionElement.elementName, DataExtensionElement.namespace, DataItemExtensionProvider())

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