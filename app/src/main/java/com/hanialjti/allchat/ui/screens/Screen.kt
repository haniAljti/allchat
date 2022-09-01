package com.hanialjti.allchat.ui.screens

sealed class Screen(val route: String) {
    object Conversations: Screen("conversations")
    object Chat: Screen("chat/{contactId}")
    object ImagePreview: Screen("imagePreview/{messageId}?enableInput={enableInput}")
}
