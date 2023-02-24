package com.hanialjti.allchat.presentation.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.google.accompanist.navigation.material.BottomSheetNavigator
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.ModalBottomSheetLayout
import com.google.accompanist.navigation.material.bottomSheet
import com.hanialjti.allchat.presentation.chat.ChatScreen
import com.hanialjti.allchat.presentation.component.CropImage
import com.hanialjti.allchat.presentation.conversation.ConversationsScreen
import com.hanialjti.allchat.presentation.create_chat_room.SelectInitialParticipantsScreen
import com.hanialjti.allchat.presentation.invite_users.InviteUsersScreen
import com.hanialjti.allchat.presentation.preview_attachment.MediaPreview
import com.hanialjti.allchat.presentation.ui.screens.*

@OptIn(ExperimentalMaterialNavigationApi::class)
@Composable
fun NavigationLayout(
    modifier: Modifier = Modifier,
    bottomSheetNavigator: BottomSheetNavigator,
    navController: NavHostController,
    loggedInUser: String?
) {

    val initialDestination =
        if (loggedInUser == null) Screen.SignIn.route else Screen.Conversations.route
//    LaunchedEffect(loggedInUser) {
//        if (userCredentials == null) {
//            navController.toSignInScreen()
//        }
//    }

    ModalBottomSheetLayout(
        modifier = modifier,
        bottomSheetNavigator = bottomSheetNavigator,
        sheetShape = RoundedCornerShape(topStart = 15.dp, topEnd = 15.dp)
    ) {

        NavHost(
            navController = navController,
            startDestination = initialDestination
        ) {

            composable(Screen.SignIn.route) {
                AuthenticationScreen(navController)
            }

            composable(Screen.CropImage.route) {
                CropImage()
            }

            composable(Screen.Conversations.route) {
                ConversationsScreen(navController)
            }

            composable(Screen.EditUserInfo.route) {
                EditUserInfoScreen(navController)
            }

            bottomSheet(Screen.AddContact.route) {
                AddContactScreen(navController)
            }

            bottomSheet(Screen.AddChatEntityScreen.route) {
                CreateEntityScreen(navController = navController)
            }

            composable(
                route = Screen.InviteUsersScreen.route,
                arguments = listOf(
                    navArgument("chatRoomId") { type = NavType.StringType }
                )
            ) {
                val chatRoomId = it.arguments?.getString("chatRoomId")
                chatRoomId?.let {
                    InviteUsersScreen(conversationId = chatRoomId, navController = navController)
                } ?: throw IllegalArgumentException("Chat room id must not be null")
            }

            val uri = "https://allchat.com"

            composable(
                route = Screen.Chat.route,
                arguments = listOf(
                    navArgument("contactId") { type = NavType.StringType },
                    navArgument("isGroupChat") { type = NavType.BoolType }
                ),
                deepLinks = listOf(
                    navDeepLink {
                        uriPattern = uri.plus("/chat/{contactId}?isGroupChat={isGroupChat}")
                    }
                )
            ) { backStackEntry ->
                backStackEntry.arguments?.getString("contactId")?.let { contactId ->
                    ChatScreen(
                        navController = navController,
                        contactId = contactId,
                        isGroupChat = backStackEntry.arguments?.getBoolean("isGroupChat") ?: false
                    )
                }
            }

            navigation(
                route = Screen.CreateChatRoomScreens.route,
                startDestination = CreateChatRoomNavDirection.SelectInitialParticipants.route,
            ) {

                composable(CreateChatRoomNavDirection.SelectInitialParticipants.route) {
                    SelectInitialParticipantsScreen(navController = navController)
                }

            }


            composable(
                route = Screen.ImagePreview.route,
                arguments = listOf(navArgument("messageId") { type = NavType.StringType })
            ) { backStackEntry ->
                backStackEntry.arguments?.getString("messageId")?.let { messageId ->
                    MediaPreview(
                        messageId = messageId,
                        navController = navController,
                    )
                }
            }
        }
    }

}


fun NavController.toChatScreen(contactId: String, isGroupChat: Boolean) {
    navigate(
        Screen.Chat.route
            .replace("{contactId}", contactId)
            .replace("{isGroupChat}", isGroupChat.toString())
    )
}

fun NavController.toCreateChatRoomScreens() {
    navigate(Screen.CreateChatRoomScreens.route)
}

fun NavController.toEditUserInfoScreen() {
    navigate(Screen.EditUserInfo.route)
}

fun NavController.toImagePreviewScreen(messageId: String) {
    navigate(
        Screen.ImagePreview.route
            .replace("{messageId}", messageId)
    )
}

fun NavController.toInviteUsersScreen(chatRoomId: String) {
    navigate(
        Screen.InviteUsersScreen.route
            .replace("{chatRoomId}", chatRoomId)
    )
}

fun NavController.toCreateEntityScreen() = navigate(Screen.AddChatEntityScreen.route)

fun NavController.toConversationsScreen() = navigate(
    Screen.Conversations.route,
    navOptions {
        popUpTo(Screen.SignIn.route) { inclusive = true }
    }
)

fun NavController.toSignInScreen() = navigate(
    Screen.SignIn.route,
    navOptions {
        popUpTo(Screen.SignIn.route) { inclusive = true }
    }
)

fun NavController.toAddNewContactScreen() = navigate(Screen.AddContact.route)