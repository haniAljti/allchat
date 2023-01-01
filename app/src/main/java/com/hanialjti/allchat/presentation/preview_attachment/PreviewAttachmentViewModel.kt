package com.hanialjti.allchat.presentation.preview_attachment

import androidx.compose.runtime.State
import com.hanialjti.allchat.presentation.chat.Attachment

interface PreviewAttachmentViewModel {
    fun getAttachment(): State<Attachment?>
    fun deleteAttachment()
}