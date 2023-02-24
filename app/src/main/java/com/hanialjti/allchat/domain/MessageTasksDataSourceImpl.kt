package com.hanialjti.allchat.domain

import androidx.work.*
import com.hanialjti.allchat.data.tasks.MessageTasksDataSource
import com.hanialjti.allchat.domain.worker.SendMessageWorker
import java.util.concurrent.TimeUnit

class MessageTasksDataSourceImpl(
    private val workManager: WorkManager
): MessageTasksDataSource {

    private fun createSendMessageWorkRequest() =
        OneTimeWorkRequestBuilder<SendMessageWorker>()
//            .setInputData(workDataOf(SendMessageWorker.MESSAGE_ID_KEY to messageId))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

    override fun sendQueuedMessages() {
        val workRequest = createSendMessageWorkRequest()
        workManager.enqueueUniqueWork(
            "send-queued-messages",
            ExistingWorkPolicy.REPLACE, workRequest
        )
    }

}