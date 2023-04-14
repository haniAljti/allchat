package com.hanialjti.allchat.presentation.conversation

import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.core.net.toFile
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.hanialjti.allchat.R

sealed class ContactImage {
    class DynamicImage(val imageCacheUri: String) : ContactImage()
    class DynamicRawImage(val bytes: ByteArray) : ContactImage()
    open class ImageRes(@DrawableRes val drawableRes: Int) : ContactImage()

    open class DefaultProfileImage(isGroupChat: Boolean) :
        ImageRes(if (isGroupChat) R.drawable.ic_group else R.drawable.ic_user)

    object DefaultUserImage: DefaultProfileImage(false)
    object DefaultGroupImage: DefaultProfileImage(true)

    @Composable
    fun AsImage(modifier: Modifier = Modifier) = when (this) {
        is DynamicImage ->
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest
                        .Builder(LocalContext.current)
                        .size(50, 50)
                        .data(Uri.parse(imageCacheUri).toFile())
                        .build()
                ),
                contentDescription = null,
                modifier = modifier.clip(CircleShape),
                contentScale = ContentScale.Crop
            )

        is ImageRes -> Box(
            modifier = modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Icon(
                painter = painterResource(id = drawableRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        is DynamicRawImage ->
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest
                        .Builder(LocalContext.current)
                        .size(50, 50)
                        .data(bytes)
                        .build()
                ),
                contentDescription = null,
                modifier = modifier.clip(CircleShape),
                contentScale = ContentScale.Crop
            )
//            Image(
//            painter = painterResource(id = drawableRes),
//            contentDescription = null,
//            colorFilter = ColorFilter.tint(MaterialTheme.colors.primary),
//            modifier = modifier
//                .border(width = 1.dp, color = MaterialTheme.colors.primary, shape = CircleShape)
//                .clip(CircleShape)
//        )
    }
}

sealed class ContactContent(val text: UiText) {
    class LastMessage(text: UiText, val read: Boolean) : ContactContent(text)
    class Composing(text: UiText) : ContactContent(text)
}

sealed class UiText {
    data class DynamicString(val value: String) : UiText()
    class StringResource(
        @StringRes val resId: Int,
        vararg val args: Any
    ) : UiText()

    class PluralStringResource(
        @PluralsRes val resId: Int,
        val count: Int,
        vararg val args: Any
    ) : UiText()

    @Composable
    fun asString(): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> stringResource(resId, *args)
            is PluralStringResource -> pluralStringResource(
                id = resId,
                count = count,
                formatArgs = args
            )
        }
    }
}