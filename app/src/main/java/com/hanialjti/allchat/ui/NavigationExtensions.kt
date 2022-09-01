package com.hanialjti.allchat.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.google.accompanist.navigation.material.BottomSheetNavigator
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.ModalBottomSheetLayout
import com.hanialjti.allchat.ui.screens.ChatScreen
import com.hanialjti.allchat.ui.screens.ConversationsScreen
import com.hanialjti.allchat.ui.screens.ImagePreviewScreen
import com.hanialjti.allchat.ui.screens.Screen

@OptIn(ExperimentalMaterialNavigationApi::class)
@Composable
fun NavigationLayout(
    bottomSheetNavigator: BottomSheetNavigator,
    navController: NavHostController
) {
    ModalBottomSheetLayout(
        bottomSheetNavigator = bottomSheetNavigator,
        sheetShape = RoundedCornerShape(topStart = 15.dp, topEnd = 15.dp)
    ) {

        NavHost(
            navController = navController,
            startDestination = Screen.Conversations.route
        ) {


            composable(Screen.Conversations.route) {
                ConversationsScreen(navController)
            }


            composable(
                route = Screen.Chat.route,
                arguments = listOf(
                    navArgument("contactId") { type = NavType.StringType },
                )
            ) { backStackEntry ->
                backStackEntry.arguments?.getString("contactId")?.let { contactId ->
                    ChatScreen(
                        navController = navController,
                        contactId = contactId,
                        viewModel = hiltViewModel(backStackEntry)
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
                            enableInput = backStackEntry.arguments?.getBoolean("enableInput") ?: false,
                            navController = navController,
                            viewModel = hiltViewModel(it)
                        )
                    }
                }
            }
        }
    }
}

fun NavController.toChatScreen(contactId: String) {
    navigate(Screen.Chat.route.replace("{contactId}", contactId))
}

fun NavController.toImagePreviewScreen(messageId: String, enableInput: Boolean = false) {
    navigate(
        Screen.ImagePreview.route
            .replace("{messageId}", messageId)
            .replace("{enableInput}", enableInput.toString())
    )
}