package com.hanialjti.allchat.data.tasks

interface MessageTasksDataSource {
    fun sendQueuedMessages()
}