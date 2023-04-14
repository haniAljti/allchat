package com.hanialjti.allchat.presentation.chat_entity_details.user_details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.hanialjti.allchat.R
import com.hanialjti.allchat.di.getViewModel
import com.hanialjti.allchat.presentation.chat_entity_details.common.ChatEntityDetailsScreen
import org.koin.core.parameter.parametersOf

@Composable
fun UserDetailsScreen(
    id: String,
    navController: NavHostController,
    viewModel: UserDetailsViewModel = getViewModel(parameters = { parametersOf(id) })
) {


    val uiState = viewModel.userDetailsUiState.collectAsState()

    ChatEntityDetailsScreen(
        onBackClicked = { navController.popBackStack() },
        onUpdateChatClicked = {  },
        isUpdateButtonVisible = false,
        avatar = uiState.value.avatar,
        name = uiState.value.name
    ) {
        uiState.value.status?.let { desc ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(30))
                    .background(Color(0xFF3E5A55))
                    .padding(15.dp)
            ) {

                Text(
                    text = "Status",
                    fontSize = 16.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(bottom = 5.dp)
                        .alpha(0.5f)
                )

                Text(
                    text = desc,
                    fontSize = 16.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        var isChecked by remember {
            mutableStateOf(false)
        }

        Column(
            modifier = Modifier.padding(horizontal = 30.dp),
            verticalArrangement = Arrangement.spacedBy(25.dp)
        ) {
            UserDetailsOptionWithSwitch(
                iconRes = R.drawable.ic_mute,
                text = "Mute Notifications",
                isChecked = isChecked,
                onCheckedChange = { isChecked = it }
            )

            UserDetailsOption(
                iconRes = R.drawable.ic_close,
                text = "Block"
            )

        }
    }
}