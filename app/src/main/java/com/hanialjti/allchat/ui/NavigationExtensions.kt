package com.hanialjti.allchat.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.google.accompanist.navigation.material.BottomSheetNavigator
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.ModalBottomSheetLayout
import com.google.accompanist.navigation.material.bottomSheet
import com.hanialjti.allchat.CustomKoin
import com.hanialjti.allchat.datastore.UserPreferencesManager
import com.hanialjti.allchat.models.UserCredentials
import com.hanialjti.allchat.ui.screens.*
import org.koin.androidx.compose.getViewModel
import org.koin.core.component.get

@OptIn(ExperimentalMaterialNavigationApi::class)
@Composable
fun NavigationLayout(
    modifier: Modifier = Modifier,
    bottomSheetNavigator: BottomSheetNavigator,
    navController: NavHostController,
    userCredentials: UserCredentials?
) {
    ModalBottomSheetLayout(
        modifier = modifier,
        bottomSheetNavigator = bottomSheetNavigator,
        sheetShape = RoundedCornerShape(topStart = 15.dp, topEnd = 15.dp)
    ) {

        LaunchedEffect(userCredentials) {
            if (userCredentials == null) {
                navController.toSignInScreen()
            }
        }
        
        NavHost(
            navController = navController,
            startDestination = Screen.Conversations.route
        ) {

            composable(Screen.SignIn.route) {
                AuthenticationScreen(navController)
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

            composable(
                route = Screen.Chat.route,
                arguments = listOf(
                    navArgument("contactId") { type = NavType.StringType },
                    navArgument("isGroupChat") { type = NavType.BoolType }
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

            composable(
                route = Screen.ImagePreview.route,
                arguments = listOf(
                    navArgument("messageId") { type = NavType.StringType },
                    navArgument("enableInput") { type = NavType.BoolType }
                )
            ) { backStackEntry ->
                backStackEntry.arguments?.getString("messageId")?.let { messageId ->
                    navController.previousBackStackEntry?.let {
                        ImagePreviewScreen(
                            messageId = messageId,
                            enableInput = backStackEntry.arguments?.getBoolean("enableInput")
                                ?: false,
                            navController = navController,
                            viewModel = getViewModel(
                                scope = CustomKoin.getScope(),
                                owner = it,
                            )
                        )
                    }
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

fun NavController.toEditUserInfoScreen() {
    navigate(Screen.EditUserInfo.route)
}

fun NavController.toImagePreviewScreen(messageId: String, enableInput: Boolean = false) {
    navigate(
        Screen.ImagePreview.route
            .replace("{messageId}", messageId)
            .replace("{enableInput}", enableInput.toString())
    )
}

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