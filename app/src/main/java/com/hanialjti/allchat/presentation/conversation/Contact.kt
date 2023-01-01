package com.hanialjti.allchat.presentation.conversation

import androidx.annotation.DrawableRes
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest

sealed class ContactImage {
    class DynamicImage(val imageUrl: String) : ContactImage()
    class ImageRes(@DrawableRes val drawableRes: Int) : ContactImage()

    @Composable
    fun AsImage(modifier: Modifier) = when (this) {
        is DynamicImage ->
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest
                        .Builder(LocalContext.current)
                        .size(50, 50)
                        .data(imageUrl)
                        .build()
                ),
                contentDescription = null,
                modifier = modifier.clip(CircleShape),
                contentScale = ContentScale.Crop
            )

        is ImageRes -> Image(
            painter = painterResource(id = drawableRes),
            contentDescription = null,
            colorFilter = ColorFilter.tint(MaterialTheme.colors.primary),
            modifier = modifier
                .border(width = 3.dp, color = MaterialTheme.colors.primary, shape = CircleShape)
                .clip(CircleShape)
        )
    }
}

sealed class ContactContent(val text: UiText) {
    class LastMessage(text: UiText, val read: Boolean): ContactContent(text)
    class Composing(text: UiText): ContactContent(text)
}

sealed class UiText {
    data class DynamicString(val value: String): UiText()
    class StringResource(
        @StringRes val resId: Int,
        vararg val args: Any
    ): UiText()
    class PluralStringResource(
        @PluralsRes val resId: Int,
        val count: Int,
        vararg val args: Any
    ): UiText()

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun asString(): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> stringResource(resId, *args)
            is PluralStringResource -> pluralStringResource(id = resId, count = count, formatArgs = args)
        }
    }
}