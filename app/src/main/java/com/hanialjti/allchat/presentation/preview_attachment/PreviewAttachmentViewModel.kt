package com.hanialjti.allchat.presentation.preview_attachment

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanialjti.allchat.data.model.MessageItem
import com.hanialjti.allchat.data.repository.IMessageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PreviewAttachmentViewModel(
    private val messageId: String,
    private val chatRepository: IMessageRepository

): ViewModel() {

    private val _previewAttachmentUiState = MutableStateFlow(PreviewAttachmentUiState())
    val previewAttachmentUiState: StateFlow<PreviewAttachmentUiState> get() = _previewAttachmentUiState

    @RequiresApi(Build.VERSION_CODES.Q)
    fun downloadAttachment(messageData: MessageItem.MessageData) {
        viewModelScope.launch {
            chatRepository.downloadAttachment(messageData)
        }
    }
    init {
        viewModelScope.launch {
            chatRepository.getMessageByExternalId(messageId)
                ?.collectLatest { message ->
                    _previewAttachmentUiState.update {
                        it.copy(message = message)
                    }
                }
        }
    }
}

data class PreviewAttachmentUiState(
    val message: MessageItem.MessageData? = null
)