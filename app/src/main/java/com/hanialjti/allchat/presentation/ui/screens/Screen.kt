package com.hanialjti.allchat.presentation.ui.screens

import androidx.annotation.DrawableRes
import com.hanialjti.allchat.R

sealed class Screen(val route: String, val title: String, @DrawableRes val iconRes: Int?) {
    object Conversations : Screen("conversations", "Chats", R.drawable.ic_messages)
    object Chat : Screen("chat/{contactId}?isGroupChat={isGroupChat}", "Active Chat", null)
    object ImagePreview : Screen("imagePreview/{messageId}", "Image Preview", null)
    object EditUserInfo : Screen("editUserInfo", "Edit User", R.drawable.ic_user)
    object AddContact : Screen("addNewContact", "Add Contact", null)
    object SignIn : Screen("signIn", "Sign In", null)
    object InviteUsersScreen : Screen("inviteUsers/{chatRoomId}", "Invite Users", null)
    object AddChatEntityScreen : Screen("addEntity", "Add Entry", null)
    object CreateChatRoomScreens : Screen("createChatRoom", "Create Chat Room", null)
    object CropImage : Screen("cropper", "Crop Image", null)
    object ChatDetailsScreen : Screen("chat/details/{id}", "Chat Details", null)
    object UpdateChatDetailsScreen : Screen("chat/{id}/update", "Update Chat Details", null)
    object UserDetailsScreen : Screen("user/details/{id}", "User Details", null)
}

sealed class CreateChatRoomNavDirection(route: String, title: String, @DrawableRes iconRes: Int?) : Screen(route, title, iconRes) {
    object InputRoomInfo : Screen("inputRoomInfo", "Input Room Info", null)
    object SelectInitialParticipants : Screen("selectParticipants", "Select Participants", null)
}

