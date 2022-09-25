package com.hanialjti.allchat.domain.worker

//class AddContactWorker(
//    context: Context,
//    private val parameters: WorkerParameters,
//    private val chatRepository: XmppChatRepository,
//    private val connectionHelper: XmppConnectionHelper,
//    private val connectionManager: ConnectionManager
//) : CoroutineWorker(
//    context,
//    parameters
//) {
//    override suspend fun doWork(): Result {
//        val messageId = parameters.inputData.getLong(CONTACT_ID_KEY, -1).toInt()
//
//        if (messageId == -1) return Result.failure()
//
//        connectionManager.registerWorker(this)
//
//        val message = chatRepository.sendMessageAndRegisterForAcknowledgment(messageId)
//
//        connectionManager.unregisterWorker(this)
//        return when (message) {
//            is Resource.Error -> {
//                Result.retry()
//            }
//            is Resource.Success -> {
//                Result.success()
//            }
//            else -> {
//                Result.retry()
//            }
//        }
//
//    }
//
//    companion object {
//        const val CONTACT_ID_KEY = "contact-id"
//    }
//
//    override suspend fun getForegroundInfo(): ForegroundInfo {
//        return ForegroundInfo(
//            50,
//            buildNotification()
//        )
//    }
//
//    private fun buildNotification(): Notification {
//
//        val notificationManager =
//            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//
//        val messagingChannel = NotificationChannel(
//            "add-contact-service", "Chat", NotificationManager.IMPORTANCE_MIN
//        )
//
//        notificationManager.createNotificationChannel(messagingChannel)
//
//        return NotificationCompat.Builder(applicationContext, "add-contact-service")
//            .setOngoing(true)
//            .setSmallIcon(R.drawable.ic_user)
//            .setPriority(NotificationCompat.PRIORITY_MIN)
//            .setCategory(Notification.CATEGORY_SERVICE)
//            .build()
//    }
//}