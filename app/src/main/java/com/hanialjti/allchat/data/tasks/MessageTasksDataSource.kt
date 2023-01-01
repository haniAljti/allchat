package com.hanialjti.allchat.data.tasks

interface MessageTasksDataSource {
    fun createAndExecuteSendMessageWork(messageId: Long)
}