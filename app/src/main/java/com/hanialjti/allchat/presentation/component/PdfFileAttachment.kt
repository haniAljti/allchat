package com.hanialjti.allchat.presentation.component

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.hanialjti.allchat.R
import com.hanialjti.allchat.data.model.Media
import com.hanialjti.allchat.data.remote.model.DownloadProgress

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PdfFileAttachment(
    pdf: Media,
    downloadProgress: DownloadProgress?,
    onPdfClicked: () -> Unit,
    modifier: Modifier
) {

    val isDownloaded by remember(pdf) { mutableStateOf(pdf.cacheUri != null) }

    Row(
        modifier = modifier
            .clickable { onPdfClicked() }
            .fillMaxWidth()
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically) {

        AnimatedContent(
            targetState = isDownloaded,
            transitionSpec = { scaleIn() with scaleOut() }
        ) {
            val boxModifier = Modifier
                .background(color = Color(0x4D191D18), shape = RoundedCornerShape(15.dp))
                .size(45.dp)
            if (it) {
                Box(modifier = boxModifier) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_document),
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            } else {

                Box(modifier = boxModifier) {
                    if (downloadProgress != null) {
                        CircularProgressIndicator(
                            progress = (downloadProgress.downloadedBytes.toFloat() / downloadProgress.totalBytes),
                            modifier = Modifier.padding(10.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_down),
                            contentDescription = null,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

        }

        Spacer(modifier = Modifier.width(10.dp))
        pdf.fileName?.let { Text(text = it, color = Color.White) }
    }
}