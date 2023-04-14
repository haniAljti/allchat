package com.hanialjti.allchat.presentation.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.hanialjti.allchat.R
import com.hanialjti.allchat.data.model.Media

@Composable
fun ImageAttachment(
    image: Media,
    onImageClicked: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color,
    containerColor: Color,
    timestampAndStatus: @Composable () -> Unit
) {

    val context = LocalContext.current
    val imageSource = image.cacheUri ?: image.url

    val imagePainter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(imageSource)
            .build(),
        contentScale = ContentScale.FillHeight
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {

        AnimatedContent(
            targetState = imagePainter.state,
            modifier = Modifier.align(Alignment.Center)
        ) {
            when (it) {
                is AsyncImagePainter.State.Error -> {
                    Icon(
                        painterResource(id = R.drawable.ic_image_corrupt),
                        contentDescription = null,
                        modifier = Modifier.size(50.dp),
                        tint = contentColor
                    )
                }
                is AsyncImagePainter.State.Loading -> {
                    CircularProgressIndicator(color = contentColor)
                }
                else -> {

                }
            }
        }

        Image(
            painter = imagePainter,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .clip(RoundedCornerShape(10))
                .fillMaxWidth()
                .clickable { onImageClicked() }
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(5.dp)
                .clip(shape = RoundedCornerShape(50))
                .background(color = containerColor.copy(alpha = 0.4f))
        ) {
            timestampAndStatus()
        }
    }
}