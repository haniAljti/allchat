package com.hanialjti.allchat.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.hanialjti.allchat.R
import com.hanialjti.allchat.data.model.Media

@Composable
fun ImageAttachment(
    image: Media,
    onImageClicked: () -> Unit,
    modifier: Modifier = Modifier,
    timestampAndStatus: @Composable () -> Unit
) {

    val imageSource = image.cacheUri ?: image.url

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {

        Image(
            painter = rememberAsyncImagePainter(
                model = ImageRequest
                    .Builder(LocalContext.current)
                    .data(imageSource)
                    .build(),
                fallback = painterResource(id = R.drawable.ic_launcher_foreground),
                error = rememberAsyncImagePainter(R.drawable.ic_image_corrupt, ),
                contentScale = ContentScale.FillHeight
            ),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .fillMaxWidth()
                .clickable { onImageClicked() }
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(5.dp)
                .clip(shape = RoundedCornerShape(50))
                .background(color = Color(0x66191D18))
//                .padding(5.dp)
        ) {
            timestampAndStatus()
        }
    }
}