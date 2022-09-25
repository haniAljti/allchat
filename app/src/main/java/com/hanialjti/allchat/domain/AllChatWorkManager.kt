package com.hanialjti.allchat.domain

import androidx.work.*
import com.hanialjti.allchat.domain.worker.SendMessageWorker
import java.util.concurrent.TimeUnit

class AllChatWorkManager(
    private val workManager: WorkManager
) {

    private fun createSendMessageWorkRequest(messageId: Long) =
        OneTimeWorkRequestBuilder<SendMessageWorker>()
            .setInputData(workDataOf(SendMessageWorker.MESSAGE_ID_KEY to messageId))
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

    fun createAndExecuteSendMessageWork(messageId: Long) {
        val workRequest = createSendMessageWorkRequest(messageId)
        enqueueWorkRequest(workRequest, messageId.toString())
    }

    private fun enqueueWorkRequest(workRequest: OneTimeWorkRequest, workName: String) {
        workManager.enqueueUniqueWork(workName, ExistingWorkPolicy.REPLACE, workRequest)
    }
}