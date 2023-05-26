package com.hanialjti.allchat.data.tasks

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkRequest
import java.util.concurrent.TimeUnit

class MessageTasksDataSourceImpl(
    private val workManager: WorkManager
): MessageTasksDataSource {

    private fun createSendMessageWorkRequest() =
        OneTimeWorkRequestBuilder<SendMessageWorker>()
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