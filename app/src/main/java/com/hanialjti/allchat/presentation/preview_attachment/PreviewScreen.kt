package com.hanialjti.allchat.presentation.preview_attachment

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.hanialjti.allchat.R
import com.hanialjti.allchat.data.model.Media
import com.hanialjti.allchat.di.getViewModel
import com.hanialjti.allchat.presentation.component.TopBar
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf
import java.io.File

@Composable
fun MediaPreview(
    messageId: String,
    navController: NavHostController,
    viewModel: PreviewAttachmentViewModel = getViewModel(parameters = { parametersOf(messageId) })
) {

    val uiState by remember(viewModel) { viewModel.previewAttachmentUiState }.collectAsState()
    val media by remember(uiState) { mutableStateOf(uiState.message?.attachment as? Media) }
    var autoOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(media, autoOpen) {
        if (autoOpen) {
            media?.cacheUri?.let {
                scope.launch { context.openImageWithChooser(it, media?.mimeType) }
            }
            autoOpen = false
        }
    }


//    val isPreviewPossible by remember {
//        derivedStateOf {
//            val attachment = uiState.message?.attachment
//            attachment != null && attachment is Media &&
//                    (attachment.type == Attachment.Type.Image || attachment.type == Attachment.Type.Video)
//        }
//    }

//    if (!isPreviewPossible) {
//        Logger.d { "Attachment can not be previewed" }
//        navController.popBackStack()
//    }

    Column(Modifier.background(androidx.compose.material.MaterialTheme.colors.background)) {


        TopBar(
            title = "",
            onBackClicked = { navController.popBackStack() },
            modifier = Modifier.height(75.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(20.dp)
            ) {

                IconButton(
                    onClick = {
                        if (media?.cacheUri == null) {
                            uiState.message?.let {
                                viewModel.downloadAttachment(it)
                            }
                        }
                        autoOpen = true
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_open_in_gallery),
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }


        Image(
            painter = rememberAsyncImagePainter(
                media?.cacheUri ?: media?.url,
                contentScale = ContentScale.FillWidth
            ),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

    }

}

private fun Context.openImageWithChooser(
    source: String,
    mimeType: String?
) {
    val file = File(source)
    val uri = file.let {
        FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            it
        )
    }
    val openFileIntent = Intent(Intent.ACTION_VIEW)
    mimeType?.let { openFileIntent.setDataAndType(uri, mimeType) }
    openFileIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

    val chooserIntent = Intent
        .createChooser(
            openFileIntent,
            getString(R.string.choose_pdf_title)
        )

    try {
        startActivity(chooserIntent)
    } catch (e: ActivityNotFoundException) {
        e.printStackTrace()
    }
}