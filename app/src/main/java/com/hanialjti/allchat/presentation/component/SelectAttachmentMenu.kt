package com.hanialjti.allchat.presentation.component

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
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hanialjti.allchat.R
import com.hanialjti.allchat.common.model.MenuOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun OpenFilePickerSheetLayout(
    coroutine: CoroutineScope,
    bottomSheetState: ModalBottomSheetState,
    pickMedia: ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?>,
    takePhoto: ManagedActivityResultLauncher<Void?, Bitmap?>,
    choosePdfDocument: ManagedActivityResultLauncher<String, Uri?>
) {
    Spacer(modifier = Modifier.height(1.dp))

    AttachmentMenu(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colors.background)
    ) { selectedOption ->
        coroutine.launch { bottomSheetState.hide() }
        when (selectedOption) {
            is AttachmentMenuOption.OpenGallery -> {
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
            is AttachmentMenuOption.Camera -> {
                takePhoto.launch()
            }
            is AttachmentMenuOption.Document -> {
                choosePdfDocument.launch("application/*")
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
    Column(
        modifier = modifier.padding(bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BottomSheetKnob()
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            attachmentMenuOptions.forEach { attachmentMenuOption ->
                MenuOption(attachmentMenuOption) { menuOption ->
                    onOptionSelected(menuOption)
                }
            }
        }
    }
}

sealed class AttachmentMenuOption(
    @StringRes override val name: Int,
    override val icon: Int
) :
    MenuOption(name, icon) {
    object OpenGallery : MenuOption(R.string.gallery, R.drawable.ic_gallery)
    object Camera : MenuOption(R.string.camera, R.drawable.ic_camera)
    object Document : MenuOption(R.string.document, R.drawable.ic_document)
    object Location : MenuOption(R.string.location, R.drawable.ic_location)

}

val attachmentMenuOptions = listOf(
    AttachmentMenuOption.Camera,
    AttachmentMenuOption.OpenGallery,
    AttachmentMenuOption.Document,
    AttachmentMenuOption.Location
)

@Composable
fun MenuOption(
    menuOption: MenuOption,
    modifier: Modifier = Modifier,
    onOptionSelected: (MenuOption) -> Unit
) {
    Column(
        modifier = modifier
            .width(75.dp)
            .clickable { onOptionSelected(menuOption) },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF191D18))
                .padding(14.dp)
        ) {
            Icon(
                painter = painterResource(id = menuOption.icon),
                contentDescription = null,
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(text = stringResource(id = menuOption.name), textAlign = TextAlign.Center)
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