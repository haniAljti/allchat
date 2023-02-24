package com.hanialjti.allchat.data.remote.xmpp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.SystemClock
import com.hanialjti.allchat.common.utils.Logger
import org.jivesoftware.smack.Manager
import org.jivesoftware.smack.XMPPConnection
import org.jivesoftware.smack.XMPPConnectionRegistry
import org.jivesoftware.smack.util.Async
import org.jivesoftware.smackx.ping.PingManager
import java.util.*
import kotlin.collections.HashSet

/**
 * Send automatic server pings with the help of [AlarmManager].
 *
 *
 * Smack's [PingManager] uses a `ScheduledThreadPoolExecutor` to schedule the
 * automatic server pings, but on Android, those scheduled pings are not reliable. This is because
 * the Android device may go into deep sleep where the system will not continue to run this causes
 *
 *  * the system time to not move forward, which means that the time spent in deep sleep is not
 * counted towards the scheduled delay time
 *  * the scheduled Runnable is not run while the system is in deep sleep.
 *
 *
 *
 * That is the reason Android comes with an API to schedule those tasks: AlarmManager. Which this
 * class uses to determine every 30 minutes if a server ping is necessary. The interval of 30
 * minutes is the ideal trade-off between reliability and low resource (battery) consumption.
 *
 *
 *
 * In order to use this class you need to call [.onCreate] **once**, for example
 * in the `onCreate()` method of your Service holding the XMPPConnection. And to avoid
 * leaking any resources, you should call [.onDestroy] when you no longer need any of its
 * functionality.
 *
 */
class ServerPingWithAlarmManager private constructor(connection: XMPPConnection) :
    Manager(connection) {
    /**
     * If enabled, ServerPingWithAlarmManager will call [PingManager.pingServerIfNecessary]
     * for the connection of this instance every half hour.
     *
     * @param enabled whether or not this manager is should be enabled or not.
     */
    var isEnabled = true

    companion object {

        private const val PING_ALARM_ACTION = "org.igniterealtime.smackx.ping.ACTION"
        private val INSTANCES: MutableMap<XMPPConnection, ServerPingWithAlarmManager> =
            WeakHashMap()

        init {
            XMPPConnectionRegistry.addConnectionCreationListener { connection ->
                getInstanceFor(
                    connection
                )
            }
        }

        @Synchronized
        fun getInstanceFor(connection: XMPPConnection): ServerPingWithAlarmManager {
            var serverPingWithAlarmManager = INSTANCES[connection]
            if (serverPingWithAlarmManager == null) {
                serverPingWithAlarmManager = ServerPingWithAlarmManager(connection)
                INSTANCES[connection] =
                    serverPingWithAlarmManager
            }
            return serverPingWithAlarmManager
        }

        private val ALARM_BROADCAST_RECEIVER: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Logger.d { "Ping Alarm broadcast received" }
                var managers: Set<Map.Entry<XMPPConnection, ServerPingWithAlarmManager>>
                synchronized(ServerPingWithAlarmManager::class.java) {
                    // Make a copy to avoid ConcurrentModificationException when
                    // iterating directly over INSTANCES and the Set is modified
                    // concurrently by creating a new ServerPingWithAlarmManager.
                    managers =
                        HashSet<Map.Entry<XMPPConnection, ServerPingWithAlarmManager>>(
                            INSTANCES.entries
                        )
                }
                managers.forEach { (connection, value) ->
                    if (value.isEnabled) {
                        Logger.d { "Calling pingServerIfNecessary for connection $connection" }

                        val pingManager = PingManager.getInstanceFor(connection)
                        // Android BroadcastReceivers have a timeout of 60 seconds.
                        // The connections reply timeout may be higher, which causes
                        // timeouts of the broadcast receiver and a subsequent ANR
                        // of the App of the broadcast receiver. We therefore need
                        // to call pingServerIfNecessary() in a new thread to avoid
                        // this. It could happen that the device gets back to sleep
                        // until the Thread runs, but that's a risk we are willing
                        // to take into account as it's unlikely.
                        Async.go(
                            { pingManager.pingServerIfNecessary() },
                            "PingServerIfNecessary (" + connection.connectionCounter + ')'
                        )
                    } else {
                        Logger.d {
                            "NOT calling pingServerIfNecessary (disabled) on connection " +
                                    "${connection.connectionCounter}"
                        }
                    }
                }
            }
        }
        private var sPendingIntent: PendingIntent? = null
        private var sAlarmManager: AlarmManager? = null

        /**
         * Register a pending intent with the AlarmManager to be broadcasted every half hour and
         * register the alarm broadcast receiver to receive this intent. The receiver will check all
         * known questions if a ping is Necessary when invoked by the alarm intent.
         *
         * @param context an Android context.
         */
        fun onCreate(context: Context) {
            context.registerReceiver(ALARM_BROADCAST_RECEIVER, IntentFilter(PING_ALARM_ACTION))
            sAlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            sPendingIntent = PendingIntent.getBroadcast(context, 0, Intent(PING_ALARM_ACTION), PendingIntent.FLAG_IMMUTABLE)
            sAlarmManager?.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 1000,
                AlarmManager.INTERVAL_HALF_HOUR, sPendingIntent
            )
        }

        /**
         * Unregister the alarm broadcast receiver and cancel the alarm.
         */
        fun onDestroy(context: Context) {
            context.unregisterReceiver(ALARM_BROADCAST_RECEIVER)
            sAlarmManager?.cancel(sPendingIntent)
        }
    }
}