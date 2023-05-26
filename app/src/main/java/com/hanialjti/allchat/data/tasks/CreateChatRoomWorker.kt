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
import com.hanialjti.allchat.data.repository.AuthRepository
import com.hanialjti.allchat.data.repository.ConversationRepository

class CreateChatRoomWorker(
    context: Context,
    private val parameters: WorkerParameters,
    private val conversationRepository: ConversationRepository,
    private val authenticationRepository: AuthRepository
) : CoroutineWorker(
    context,
    parameters
) {
    override suspend fun doWork(): Result {
        val chatRoomId = parameters.inputData.getString(CONTACT_ID_KEY) ?: return Result.failure()

        authenticationRepository.connectAndDelayRetry(1)

//        return when (conversationRepository.createChatRoom(chatRoomId)) {
//            is CallResult.Success -> {
//                Result.success()
//            }
//            else -> {
//                Result.retry()
//            }
//        }

        return Result.failure()
    }

    companion object {
        const val CONTACT_ID_KEY = "contact-id"
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
            "add-contact-service", "Chat", NotificationManager.IMPORTANCE_MIN
        )

        notificationManager.createNotificationChannel(messagingChannel)

        return NotificationCompat.Builder(applicationContext, "add-contact-service")
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_user)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }
}