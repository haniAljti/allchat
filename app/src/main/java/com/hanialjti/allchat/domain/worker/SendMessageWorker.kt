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
import com.hanialjti.allchat.common.exception.NotAuthenticatedException
import com.hanialjti.allchat.data.remote.model.CallResult
import com.hanialjti.allchat.data.remote.ConnectionManager
import com.hanialjti.allchat.data.remote.registerWorker
import com.hanialjti.allchat.data.repository.IChatRepository
import timber.log.Timber

class SendMessageWorker(
    context: Context,
    private val parameters: WorkerParameters,
    private val chatRepository: IChatRepository,
    private val connectionManager: ConnectionManager
) : CoroutineWorker(context, parameters) {


    override suspend fun doWork(): Result {
        Timber.d("Sending message...")
        val messageId = parameters.inputData.getLong(MESSAGE_ID_KEY, -1).toInt()

        Timber.d("Sending message with id '$messageId'...")
        if (messageId == -1) {
            Timber.d("Error: Message not found")
            return Result.failure()
        }

        try {
            val message = connectionManager.registerWorker(this) {
                chatRepository.sendMessageAndRegisterForAcknowledgment(messageId)
            }

            return when (message) {
                is CallResult.Success -> {
                    Timber.d("Message with id '$messageId' Sent successfully with external id '${message.data}'")
                    Result.success()
                }
                else -> {
                    Timber.d("Failed to send message with id '$messageId'!. Will retry later...")
                    Result.retry()
                }
            }

        } catch (e: Exception) {
            Timber.e("Failed to send message with id '$messageId'!. Will retry later...", e)
            return Result.retry()
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