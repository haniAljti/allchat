package com.hanialjti.allchat.data.tasks

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import com.hanialjti.allchat.R
import com.hanialjti.allchat.common.utils.ApplicationUtils
import com.hanialjti.allchat.common.utils.Logger
import com.hanialjti.allchat.data.remote.xmpp.ServerPingWithAlarmManager
import com.hanialjti.allchat.data.repository.IMessageRepository
import com.hanialjti.allchat.di.AllChat
import com.hanialjti.allchat.presentation.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filter
import org.jivesoftware.smack.tcp.XMPPTCPConnection

class ChatForegroundService: Service() {

    private lateinit var serverPingWithAlarmManager: ServerPingWithAlarmManager
    private lateinit var connection: XMPPTCPConnection
    private lateinit var messageRepository: IMessageRepository

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    companion object {
        private const val CHANNEL_ID = "chat_foreground"
        private const val NOTIFICATION_ID = 325461
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        connection = AllChat.getKoinApplication().koin.get()
        messageRepository = AllChat.getKoinApplication().koin.get()

        ServerPingWithAlarmManager.onCreate(this)
        serverPingWithAlarmManager = ServerPingWithAlarmManager.getInstanceFor(connection)
        serverPingWithAlarmManager.isEnabled = true

        scope.launch {
            messageRepository.messageUpdatesStream()
                .filter {
                    it.sentTo == null
                }
                .collect {
                    if (ApplicationUtils.isInBackground) {

//                        val user = it.senderId?.let { it1 -> userRepository.getInfoFor(it1) }
//                        val image = user?.cachePath
//
//                        it.contactId?.let { it1 ->
//                            NotificationUtils.showNewMessageNotification(
//                                this@ChatForegroundService.applicationContext,
//                                it1,
//                                image?.let { File(it).toUri() },
//                                user?.nickname ?: "AllChat User",
//                                message = it
//                            )
//                        }
                    }
                }
        }

        Logger.d { "OnStartCommand has been called" }
        createNotificationChannel()

        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            }

        val notification: Notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getText(R.string.notification_title))
            .setContentText(getText(R.string.notification_message))
            .setSmallIcon(R.drawable.ic_logo_notification)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        ServerPingWithAlarmManager.onDestroy(this)
        scope.cancel()
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.chat_notification_channel_name)
            val descriptionText = getString(R.string.chat_notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}