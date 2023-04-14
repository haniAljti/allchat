package com.hanialjti.allchat.data.repository

import android.media.MediaMetadataRetriever
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.paging.*
import com.hanialjti.allchat.R
import com.hanialjti.allchat.data.local.room.AllChatLocalRoomDatabase
import com.hanialjti.allchat.data.local.room.entity.MessageEntity
import com.hanialjti.allchat.data.local.room.entity.asMessage
import com.hanialjti.allchat.data.local.room.entity.asNetworkMessage
import com.hanialjti.allchat.data.model.*
import com.hanialjti.allchat.data.remote.*
import com.hanialjti.allchat.data.remote.model.*
import com.hanialjti.allchat.data.tasks.MessageTasksDataSource
import com.hanialjti.allchat.presentation.chat.MessageRemoteMediator
import com.hanialjti.allchat.presentation.conversation.UiText
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.*
import okio.Path.Companion.toPath
import timber.log.Timber
import java.util.*

class MessageRepository(
    private val messageLocalDataStore: AllChatLocalRoomDatabase,
    private val messageTasksDataSource: MessageTasksDataSource,
    private val messageRemoteDataSource: MessageRemoteDataSource,
    private val authenticationRepository: AuthRepository,
    private val preferencesRepository: PreferencesRepository,
    private val fileRepository: FileRepository,
    private val externalScope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher
) : IMessageRepository {

    private val messageDao = messageLocalDataStore.messageDao()

    private suspend fun loggedInUser() = authenticationRepository.loggedInUserStream.first()

    @OptIn(ExperimentalPagingApi::class)
    private fun messages(conversation: String, owner: String) = Pager(
        config = PagingConfig(pageSize = 100, enablePlaceholders = true),
        remoteMediator = MessageRemoteMediator(
            owner,
            conversation,
            messageLocalDataStore,
            messageRemoteDataSource
        ),
        pagingSourceFactory = { messageDao.getMessagesByConversation(conversation, owner) }
    ).flow
        .map { messages ->
            messages
                .filter { message ->
                    message.body != null || message.attachment != null
                }
                .map {
                    val replyingTo = it.thread?.let { it1 -> getMessageById(it1) }
                    val replyingToMessage = if (replyingTo != null) {
                        ReplyingToMessage(
                            id = replyingTo.id,
                            senderName = replyingTo.senderName,
                            body = replyingTo.body,
                            attachment = replyingTo.attachment
                        )
                    } else null
                    it.asMessage().copy(
                        replyTo = replyingToMessage,
                        status = if (it.type == MessageType.Chat && it.markers.isNotEmpty()) it.markers.values.first().marker.toMessageStatus()
                        else {
                            if (it.markers.any { it.value.marker == Marker.Delivered }) MessageStatus.Delivered
                            else MessageStatus.Sent
                        }
                    )
                }
                .insertSeparators { before, after ->
                    when {
                        before == null -> null
                        after == null || before.date > after.date -> {
                            val localDate = java.time.LocalDate.parse(before.date)
                            val separatorText = when (java.time.LocalDate.now()) {
                                localDate -> UiText.StringResource(R.string.today)
                                localDate.minusDays(1) -> UiText.StringResource(R.string.yesterday)
                                else -> null
                            }
                            MessageItem.NewMessagesSeparator(
                                date = separatorText ?: UiText.DynamicString(before.date),
                                itemId = before.id + "_" + (after?.id ?: "")
                            )
                        }
                        !before.read && after.read -> MessageItem.NewMessagesSeparator(
                            itemId = before.id + "_" + after.id
                        )
                        else -> null
                    }
                }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun fetchMessagesFor(chatId: String) = preferencesRepository.loggedInUserStream
        .flatMapLatest { it?.let { messages(chatId, it) } ?: emptyFlow() }

    override suspend fun resendAllPendingMessages() {
        messageTasksDataSource.sendQueuedMessages()
    }

    override suspend fun getAllPendingMessages(): List<MessageEntity> {
        return loggedInUser()?.let { owner -> messageDao.getPendingMessagesByOwner(owner) }
            ?: listOf()
    }


    override suspend fun sendSeenMarkerForMessage(externalMessageId: String) =
        externalScope.async(dispatcher) {
            if (!preferencesRepository.clientPreferences().enableChatMarkers) {
                return@async CallResult.Success(true) // chat markers are not enabled
            }
            val message = messageDao.getMessageEntryByRemoteId(externalMessageId)
                ?: return@async CallResult.Error("Message not found, can't send marker")

            val sendResult = message
                .asNetworkMessage()
                ?.let { messageRemoteDataSource.updateMarkerForMessage(it, Marker.Seen) }

            if (sendResult is CallResult.Success) {
                updateMessage(message.copy(status = MessageStatus.Seen))
                return@async CallResult.Success(true)
            } else {
                return@async CallResult.Error("Could not send marker")
            }
        }.await()

    override suspend fun setMessagesAsRead(chatId: String) {
        externalScope.launch {
            val owner = loggedInUser()
                ?: return@launch com.hanialjti.allchat.common.utils.Logger.e { "User is not signed in" }

            messageDao.setAllMessagesAsRead(owner, chatId)
        }
    }

    override fun sendMessage(
        body: String?,
        replyingTo: String?,
        contactId: String,
        isGroupChat: Boolean,
        attachment: Attachment?,
    ) {
        externalScope.launch {
            val owner = preferencesRepository.loggedInUser()
            val messageId = UUID.randomUUID().toString()
            val newAttachment = if (attachment is com.hanialjti.allchat.data.model.Media) {
                val filePath = attachment.cacheUri?.toPath()
                val metadata = filePath?.let { fileRepository.metadataOrNull(it) }
                val savedFilePath = filePath?.let {
                    fileRepository.saveFile(
                        filePath,
                        mimeType = metadata?.mimeType,
                        fileName = metadata?.displayName
                    )
                }
                attachment.copy(
                    cacheUri = savedFilePath,
                    fileName = metadata?.displayName,
                    mimeType = metadata?.mimeType
                )
            } else attachment

            val message = MessageEntity(
                id = messageId,
                body = body,
                contactId = contactId,
                type = if (isGroupChat) MessageType.GroupChat else MessageType.Chat,
                ownerId = owner,
                senderId = owner,
                thread = replyingTo,
                attachment = newAttachment
            )
            messageDao.insertOrReplace(message.copy(read = true))
            message.contactId?.let { setMessagesAsRead(it) }

            messageTasksDataSource.sendQueuedMessages()
        }
    }

    override suspend fun sendMessageAndRegisterForAcknowledgment(messageId: String): CallResult<String> {
        val messageToSend = getMessageById(messageId)
            ?: return CallResult.Error("message not found", null)

        val sendResult = messageRemoteDataSource.sendMessage(
            message = messageToSend,
            thread = messageToSend.thread,
            isMarkable = preferencesRepository.clientPreferences().enableChatMarkers
        )

        if (sendResult is CallResult.Success) {
            val externalId = sendResult.data
            if (externalId == null) {
                com.hanialjti.allchat.common.utils.Logger.d { "Message is not sent because messageId is null" }
                return CallResult.Error("messageId is null")
            }
            updateMessage(
                messageToSend.copy(id = externalId)
            )
        }

        return sendResult
    }


    override suspend fun retrievePreviousPage(
        chatId: String,
        pageSize: Int
    ) = withContext(Dispatchers.IO) {
        val owner = loggedInUser()
            ?: return@withContext MessageQueryResult.Error(SecurityException("User is not signed in"))

        val oldestMessage = messageDao.getFirstMessageByChatId(owner, chatId)

        val messagePage = messageRemoteDataSource.getPreviousPage(
            chatId,
            oldestMessage?.asNetworkMessage(),
            pageSize
        )

        saveMessagePage(messagePage)

        return@withContext messagePage.error?.let {
            MessageQueryResult.Error(messagePage.error)
        } ?: MessageQueryResult.Success(messagePage.isComplete)
    }

    override suspend fun retrieveNextPage(
        chatId: String,
        pageSize: Int
    ) = withContext(Dispatchers.IO) {

        val owner = loggedInUser()
            ?: return@withContext MessageQueryResult.Error(SecurityException("User is not signed in"))

        val newestMessage = messageDao.getMostRecentMessageByChatId(owner, chatId)

        val messagePage = messageRemoteDataSource.getNextPage(
            chatId,
            newestMessage?.asNetworkMessage(),
            pageSize
        )

        saveMessagePage(messagePage)

        return@withContext messagePage.error?.let {
            MessageQueryResult.Error(messagePage.error)
        } ?: MessageQueryResult.Success(messagePage.isComplete)
    }

    private suspend fun syncMessages(
        afterMessage: RemoteMessage?,
        pageSize: Int
    ) = withContext(Dispatchers.IO) {
        val messagePage = messageRemoteDataSource.syncMessages(afterMessage, pageSize)

        saveMessagePage(messagePage)

        return@withContext messagePage.error?.let {
            MessageQueryResult.Error(messagePage.error)
        } ?: MessageQueryResult.Success(messagePage.isComplete)
    }

    private suspend fun handleMessage(
        message: RemoteMessage,
        owner: String
    ) {
//        val messageToUpdate = messageDao.getMessageByRemoteId(message.id)

//        val attachment = if (messageToUpdate?.attachment == null) {
//            when (val attachment = message.attachment) {
//                is Location -> com.hanialjti.allchat.data.model.Location(
//                    attachment.lat,
//                    attachment.lng
//                )
//                is Media -> {
//                    val metadata = FileUtils.metadataOrNull(attachment.url)
//                    com.hanialjti.allchat.data.model.Media(
//                        type = Attachment.Type.fromMimeType(metadata?.mimeType),
//                        url = attachment.url,
//                        cacheUri = null,
//                        fileName = metadata?.displayName,
//                        mimeType = metadata?.mimeType
//                    )
//                }
//                else -> {
//                    null
//                }
//            }
//
//        } else messageToUpdate.attachment

        updateMessage(message.asMessageEntity().copy(ownerId = owner))

//        if (message.chatId != null) {
//
//            message.markers.forEach { (userId, marker) ->
//
////                val userExists = userDao.exists(userId)
//
//                if (userId != owner
//                /**&& userExists*/
//                ) {
//                    markerDao.insertMarkersForMessagesBefore(
//                        sender = userId,
//                        marker = marker,
//                        timestamp = message.timestamp,
//                        owner = owner,
//                        chatId = message.chatId
//                    )
//                }
//            }
//
//            val highestMarker = markerDao.getLatestMarkersSentByAllParticipants(
//                messageId = message.id,
//                chatId = message.chatId
//            )
//
//            messageDao.updateStatusForMessagesBeforeTimestamp(
//                highestMarker,
//                message.timestamp,
//                owner,
//                message.chatId
//            )
//        }
    }

    private suspend fun saveMessagePage(
        messagePage: MessagePage
    ) {
        val owner = loggedInUser()

        if (owner == null) {
            Timber.e("User is not signed in")
            return
        }

        messagePage
            .messageList
            .map { message ->
                withContext(dispatcher) {
                    async {
                        when (message) {
                            is RemoteMessage -> {
                                handleMessage(message, owner)
                            }
                            is RemoteGroupInvitation -> {

                            }
                        }
                    }
                }
            }.awaitAll()

    }

    override suspend fun syncMessages(chatId: String) {
        val loggedInUser = loggedInUser() ?: return
        val mostRecentMessage = messageDao.getMostRecentMessage(loggedInUser)
        syncMessages(mostRecentMessage?.asNetworkMessage(), 50)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun messageUpdatesStream() =
        messageRemoteDataSource.messageChangesStream()
            .onEach { message ->

                when (message) {
                    is RemoteMessage -> {
                        Timber.d("received new message $message")

                        val owner = loggedInUser()

                        if (owner == null) {
                            Timber.e("User is not signed in. Received messages will not be saved")
                            return@onEach
                        }

                        handleMessage(message, owner)

                        message.chatId?.let { setMessagesAsRead(it) }


                    }
                    is RemoteGroupInvitation -> {

                    }
                }

            }
            .filter {
                it is RemoteMessage && it.markers.isEmpty()
            }
            .map {
                (it as RemoteMessage).asMessageEntity().asMessage().copy(sentTo = it.sentTo)
            }
//            .map { remoteMessageItem ->
//                if (remoteMessageItem is RemoteMessage && remoteMessageItem.markers.isEmpty() && remoteMessageItem.sentTo == connectionManager.clientId)
//                    remoteMessageItem.asMessageEntity().asMessage()
//                else null
//            }
//            .filterNotNull()


//    suspend fun observeChatStates() {
//        messageRemoteDataSource.observeChatStates()
//            .collect { chatState ->
//                val conversation =
//                    conversationsDao.getConversationByRemoteId(chatState.conversation)
//                conversation?.let {
//                    conversationsDao.update(
//                        it.copy(states = it.states.apply { put(chatState.from, chatState.state) })
//                    )
//                }
//            }
//    }

    override suspend fun updateMessage(message: MessageEntity) {
        messageDao.upsertMessage(message)
    }

    override suspend fun updateAttachment(messageId: String, attachment: Attachment) {
        messageDao.updateAttachment(messageId, attachment)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override suspend fun downloadAttachment(message: MessageItem.MessageData) {

        if (message.attachment !is com.hanialjti.allchat.data.model.Media || message.attachment.url == null) {
            Timber.e("attachment is not downloadable")
            return
        }

        val cachedUri = fileRepository.downloadAndSaveToSharedStorage(
            message.attachment.url,
            message.id
        )
        val metadataRetriever = MediaMetadataRetriever()
        val recordingDuration: Int? = try {
            metadataRetriever.setDataSource(cachedUri)
            val duration = metadataRetriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)

            duration?.toInt()
        } catch (e: RuntimeException) {
            e.printStackTrace()
            null
        } finally {
            metadataRetriever.release()
        }
        messageDao.updateAttachment(
            message.attachment.copy(
                cacheUri = cachedUri,
                fileName = fileRepository.guessFileName(message.attachment.url),
                duration = recordingDuration?.toLong()
            ),
            messageId = message.id
        )
    }

    override fun observeLastMessageNotSentByOwner(owner: String, conversationId: String) =
        messageDao.getLastMessageNotSendByOwner(owner, conversationId)

    private suspend fun getMessageById(messageId: String) = messageDao.getMessageById(messageId)

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun getMessageByExternalId(externalMessageId: String) =
        messageDao.getMessageFlowByRemoteId(externalMessageId)?.mapLatest { it.asMessage() }

    override suspend fun getMessage(messageId: String): MessageEntity? =
        messageDao.getMessageById(messageId)


}