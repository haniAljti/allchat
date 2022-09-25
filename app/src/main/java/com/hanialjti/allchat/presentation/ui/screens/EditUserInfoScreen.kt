package com.hanialjti.allchat.presentation.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.hanialjti.allchat.R
import com.hanialjti.allchat.presentation.component.TopBar
import com.hanialjti.allchat.presentation.viewmodels.EditUserInfoViewModel
import com.hanialjti.allchat.di.getViewModel

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun EditUserInfoScreen(
    navController: NavHostController,
    viewModel: EditUserInfoViewModel = getViewModel()
) {

    val uiState by remember(viewModel) { viewModel.uiState }.collectAsState()
    val filePickerSheetState =
        rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)


    Column {
        TopBar(title = "Edit My Info") {
            navController.popBackStack()
        }

        Column(modifier = Modifier.weight(1f)) {
            Image(
                painter = painterResource(id = R.drawable.ic_user),
                contentDescription = null,
                colorFilter = ColorFilter.tint(Color.White),
                modifier = Modifier
                    .padding(20.dp)
                    .border(width = 3.dp, color = Color.White, shape = CircleShape)
                    .size(100.dp)
                    .clip(CircleShape)
            )

            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::setUserName,
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = Color.White,
                ),
                label = {
                    Text("Nickname")
                }
            )

//            OutlinedTextField(
//                value = uiState.name,
//                onValueChange = viewModel::setUserName,
//                modifier = Modifier
//                    .padding(20.dp)
//                    .fillMaxWidth(),
//                singleLine = true,
//                colors = TextFieldDefaults.outlinedTextFieldColors(
//                    textColor = Color.White,
//                ),
//                label = {
//                    Text("Nickname")
//                }
//            )
        }

        Button(
            onClick = { /*TODO*/ },
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
        ) {
            Text(text = "Save")
        }


    }

}