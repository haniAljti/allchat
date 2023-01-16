package com.hanialjti.allchat.presentation.ui.screens

import androidx.annotation.DrawableRes
import com.hanialjti.allchat.R

sealed class Screen(val route: String, val title: String, @DrawableRes val iconRes: Int?) {
    object Conversations : Screen("conversations", "Chats", R.drawable.ic_messages)
    object Chat : Screen("chat/{contactId}?isGroupChat={isGroupChat}", "Active Chat", null)
    object ImagePreview : Screen("imagePreview/{messageId}?enableInput={enableInput}", "Image Preview", null)
    object EditUserInfo : Screen("editUserInfo", "Edit User", R.drawable.ic_profile)
    object AddContact : Screen("addNewContact", "Add Contact", null)
    object SignIn : Screen("signIn", "Sign In", null)
    object InviteUsersScreen : Screen("inviteUsers/{chatRoomId}", "Invite Users", null)
    object AddChatEntityScreen : Screen("addEntity", "Add Entry", null)
    object CreateChatRoomScreens : Screen("createChatRoom", "Create Chat Room", null)
}

sealed class CreateChatRoomNavDirection(route: String, title: String, @DrawableRes iconRes: Int?) : Screen(route, title, iconRes) {
    object InputRoomInfo : Screen("inputRoomInfo", "Input Room Info", null)
    object SelectInitialParticipants : Screen("selectParticipants", "Select Participants", null)
}

