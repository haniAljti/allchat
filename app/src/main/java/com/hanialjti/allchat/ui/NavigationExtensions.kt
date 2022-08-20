package com.hanialjti.allchat.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.google.accompanist.navigation.material.BottomSheetNavigator
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.ModalBottomSheetLayout
import com.hanialjti.allchat.ui.screens.ChatScreen
import com.hanialjti.allchat.ui.screens.ConversationsScreen
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


            composable(Screen.Chat.route) {
                ChatScreen(navController)
            }

        }
    }
}

fun NavController.toChatScreen() {
    navigate(Screen.Chat.route)
}