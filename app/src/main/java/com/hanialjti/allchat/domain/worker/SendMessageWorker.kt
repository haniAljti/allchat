package com.hanialjti.allchat.domain.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.hanialjti.allchat.R
import com.hanialjti.allchat.data.remote.ConnectionManager
import com.hanialjti.allchat.data.repository.XmppChatRepository
import com.hanialjti.allchat.di.CustomKoinComponent
import com.hanialjti.allchat.common.model.Resource

class SendMessageWorker(
    context: Context,
    private val parameters: WorkerParameters,
    private val chatRepository: XmppChatRepository,
    private val connectionManager: ConnectionManager
) : CoroutineWorker(
    context,
    parameters
), CustomKoinComponent {
    override suspend fun doWork(): Result {
        val messageId = parameters.inputData.getLong(MESSAGE_ID_KEY, -1).toInt()

        if (messageId == -1) return Result.failure()

        connectionManager.registerWorker(this)

        val message = chatRepository.sendMessageAndRegisterForAcknowledgment(messageId)

        connectionManager.unregisterWorker(this)
        return when (message) {
            is Resource.Error -> {
                Result.retry()
            }
            is Resource.Success -> {
                Result.success()
            }
            else -> {
                Result.retry()
            }
        }
    }

    companion object {
        const val MESSAGE_ID_KEY = "message-id"
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            50,
            buildNotification()
        )
    }

    private fun buildNotification(): Notification {

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val messagingChannel = NotificationChannel(
            "send-message-service", "Chat", NotificationManager.IMPORTANCE_MIN
        )
        notificationManager.createNotificationChannel(messagingChannel)

        return NotificationCompat.Builder(applicationContext, "send-message-service")
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_user)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }
}