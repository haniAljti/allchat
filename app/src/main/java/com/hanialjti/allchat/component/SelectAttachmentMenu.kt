package com.hanialjti.allchat.component

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hanialjti.allchat.R
import com.hanialjti.allchat.models.MenuOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun OpenFilePickerSheetLayout(
    coroutine: CoroutineScope,
    bottomSheetState: ModalBottomSheetState,
    pickMedia: ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?>,
    takePhoto: ManagedActivityResultLauncher<Void?, Bitmap?>,
    choosePdfDocument: ManagedActivityResultLauncher<Array<String>, Uri?>
) {
    Spacer(modifier = Modifier.height(1.dp))

    AttachmentMenu { selectedOption ->
        coroutine.launch { bottomSheetState.hide() }
        when (selectedOption) {
            is AttachmentMenuOption.OpenGallery -> {
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
            is AttachmentMenuOption.OpenCamera -> {
                takePhoto.launch()
            }
            is AttachmentMenuOption.SelectPdfFile -> {
                choosePdfDocument.launch(arrayOf("application/pdf"))
            }
            else -> {
            }
        }
    }
}

@Composable
fun AttachmentMenu(
    modifier: Modifier = Modifier,
    onOptionSelected: (MenuOption) -> Unit,
) {
    Column(modifier = modifier) {
        BottomSheetKnob()
        attachmentMenuOptions.forEach { attachmentMenuOption ->
            MenuOption(attachmentMenuOption) { menuOption ->
                onOptionSelected(menuOption)
            }
        }
    }
}

sealed class AttachmentMenuOption(
    @StringRes override val name: Int,
    override val icon: Int
) :
    MenuOption(name, icon) {
    object OpenGallery : MenuOption(R.string.open_gallery, R.drawable.ic_gallery)
    object OpenCamera : MenuOption(R.string.open_camera, R.drawable.ic_camera)
    object SelectPdfFile : MenuOption(R.string.choose_pdf, R.drawable.ic_filepdf)

}

val attachmentMenuOptions = listOf(
    AttachmentMenuOption.OpenCamera,
    AttachmentMenuOption.OpenGallery,
    AttachmentMenuOption.SelectPdfFile
)

@Composable
fun MenuOption(
    menuOption: MenuOption,
    modifier: Modifier = Modifier,
    onOptionSelected: (MenuOption) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOptionSelected(menuOption) }
            .padding(PaddingValues(vertical = 15.dp, horizontal = 25.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = menuOption.icon),
            contentDescription = null,
            modifier = Modifier.padding(PaddingValues(vertical = 15.dp, horizontal = 25.dp))
        )
        Text(text = stringResource(id = menuOption.name))
    }
}


@Composable
fun BottomSheetKnob() {
    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .padding(20.dp)
                .width(100.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.DarkGray)
                .align(Alignment.Center)
        )
    }
}