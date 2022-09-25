package com.hanialjti.allchat.presentation.ui.screens

sealed class Screen(val route: String) {
    object Conversations: Screen("conversations")
    object Chat: Screen("chat/{contactId}?isGroupChat={isGroupChat}")
    object ImagePreview: Screen("imagePreview/{messageId}?enableInput={enableInput}")
    object EditUserInfo: Screen("editUserInfo")
    object AddContact: Screen("addNewContact")
    object SignIn: Screen("signIn")
    object CreateChatRoom: Screen("createChatRoom")
}
