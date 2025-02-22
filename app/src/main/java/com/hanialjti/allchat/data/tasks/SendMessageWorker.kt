package com.hanialjti.allchat.data.tasks

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.hanialjti.allchat.R
import com.hanialjti.allchat.data.model.Media
import com.hanialjti.allchat.data.remote.model.CallResult
import com.hanialjti.allchat.data.repository.AuthRepository
import com.hanialjti.allchat.data.repository.FileRepository
import com.hanialjti.allchat.data.repository.IMessageRepository
import timber.log.Timber
import java.io.File

class SendMessageWorker(
    context: Context,
    parameters: WorkerParameters,
    private val chatRepository: IMessageRepository,
    private val fileRepository: FileRepository,
    private val authenticationRepository: AuthRepository
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        Timber.d("Sending queued messages...")

        try {

            authenticationRepository.connectAndDelayRetry(1)

            val messages = chatRepository.getAllPendingMessages()

            messages
                .ifEmpty { return Result.success() }
                .forEach { message ->
                    Timber.d("Sending message with id '${message.id}'...")
                    if (message.attachment != null && message.attachment is Media) {

                        message.attachment.cacheUri?.let {
                            val file = File(it)
                            val uploadResult = fileRepository.uploadFile(file)

                            if (uploadResult is CallResult.Success<String>) {
                                chatRepository.updateAttachment(
                                    message.id,
                                    attachment = message.attachment.copy(
                                        url = uploadResult.data
                                    )
                                )
                            } else return Result.retry()

                        }
                    }


                    when (chatRepository.sendMessageAndRegisterForAcknowledgment(message.id)) {
                        is CallResult.Success -> {
                            Timber.d("Message with id '${message.id}' Sent successfully!'")
                        }
                        else -> {
                            Timber.d("Failed to send message with id '${message.id}'!. Will retry later...")
                            return Result.retry()
                        }
                    }
                }

            return Result.success()

        } catch (e: Exception) {
            Timber.e("Failed to send queued messages!. Will retry later...", e)
            return Result.retry()
        }

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
            .setSmallIcon(R.drawable.ic_logo_notification)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }
}