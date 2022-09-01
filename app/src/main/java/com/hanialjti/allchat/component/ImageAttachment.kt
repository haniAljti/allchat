package com.hanialjti.allchat.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.hanialjti.allchat.R
import com.hanialjti.allchat.models.UiAttachment

@Composable
fun ImageAttachment(
    image: UiAttachment.Image,
    onImageClicked: () -> Unit,
    modifier: Modifier = Modifier
) {

    val imageSource = image.url ?: image.cacheUri

    Image(
        painter = rememberAsyncImagePainter(
            model = ImageRequest
                .Builder(LocalContext.current)
                .size(256, 200)
                .data(imageSource)
                .build(),
            fallback = painterResource(id = R.drawable.ic_launcher_foreground)
        ),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable { onImageClicked() }
    )
}