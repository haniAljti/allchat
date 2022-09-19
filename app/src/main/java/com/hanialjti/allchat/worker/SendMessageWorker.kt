package com.hanialjti.allchat.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hanialjti.allchat.models.MessageOperation
import com.hanialjti.allchat.models.entity.Status
import com.hanialjti.allchat.repository.XmppChatRepository

class SendMessageWorker(
    context: Context,
    private val parameters: WorkerParameters,
    private val chatRepository: XmppChatRepository
): CoroutineWorker(
    context,
    parameters
) {
    override suspend fun doWork(): Result {
        val messageId = parameters.inputData.getString(MESSAGE_ID_KEY)
            ?: return Result.failure()

        val message = chatRepository.getMessageById(messageId)

        val messageResult = chatRepository.sendMessage(message)

        chatRepository.handleMessageOperation(messageResult)

        return when (messageResult) {
            is MessageOperation.Error -> {
                Result.retry()
            }
            is MessageOperation.StatusChanged -> {
                if (messageResult.message.status == Status.Sent) {
                    Result.success()
                }
                Result.retry()
            }
            else -> { Result.retry() }
        }

    }

    companion object {
        const val MESSAGE_ID_KEY = "message-id"
    }

}