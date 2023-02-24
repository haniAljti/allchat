package com.hanialjti.allchat.common.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import com.hanialjti.allchat.R
import com.hanialjti.allchat.data.model.MessageItem
import com.hanialjti.allchat.presentation.MainActivity
import kotlinx.datetime.toJavaLocalDateTime
import java.time.ZoneId

object NotificationUtils {

    private const val CHANNEL_ID = "chat-messages-channel"

    private val users: MutableMap<String, NotificationCompat.MessagingStyle> = mutableMapOf()

    @RequiresApi(Build.VERSION_CODES.P)
    fun showNewMessageNotification(
        context: Context,
        chatId: String,
        senderUri: Uri?,
        senderName: String?,
        message: MessageItem.MessageData
    ) {
        createChatNotificationChannel(context)

        if (!users.containsKey(chatId)) {
            users[chatId] = NotificationCompat.MessagingStyle(
                Person.Builder()
                    .apply {
                        if (senderUri != null)
                            setIcon(IconCompat.createWithAdaptiveBitmapContentUri(senderUri))
                        if (senderName != null) {
                            setName(senderName)
                            setKey(senderName)
                        }
                    }
                    .build()
            )
//                .also { notifications ->
//                messages.forEach {
//                    notifications.addMessage(
//                        it.body,
//                        it.timestamp.toJavaLocalDateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
//                        null as? Person?
//                    )
//                }
//            }
        }

        val messagingStyle = users[chatId]?.addMessage(
            message.body,
            message.timestamp.toJavaLocalDateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            null as? Person?
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_logo_notification)
            .setStyle(
                messagingStyle
//                NotificationCompat.MessagingStyle(
//                    Person.Builder()
//                        .apply {
//                            if (senderUri != null)
//                                setIcon(IconCompat.createWithAdaptiveBitmapContentUri(senderUri))
//                            if (senderName != null) {
//                                setName(senderName)
//                                setKey(senderName)
//                            }
//                        }
//                        .build()
//                ).also { notifications ->
//                    messages.forEach {
//                        notifications.addMessage(
//                            it.body,
//                            it.timestamp.toJavaLocalDateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
//                            null as? Person?
//                        )
//                    }
//                }
            )
            .setContentIntent(createChatPendingIntent(context, chatId, true))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(context)) {
            // notificationId is a unique int for each notification that you must define
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            notify(chatId.hashCode(), builder.build())
        }
    }

    private fun createChatPendingIntent(context: Context, chatId: String, isGroupChat: Boolean): PendingIntent {
        val taskDetailIntent = Intent(
            Intent.ACTION_VIEW,
            "https://AllChatasdf.com/chat/${chatId}?isGroupChat=${isGroupChat}".toUri(),
            context,
            MainActivity::class.java
        )

        return TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(taskDetailIntent)
            getPendingIntent(123456, PendingIntent.FLAG_IMMUTABLE)
        }
    }

    private fun createChatNotificationChannel(context: Context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.chat_notification_channel_name)
            val descriptionText = context.getString(R.string.chat_notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}