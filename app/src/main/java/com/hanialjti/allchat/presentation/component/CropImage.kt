package com.hanialjti.allchat.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

@Composable
fun CropImage(

) {

    BoxWithConstraints(modifier = Modifier
        .background(Color.Green)
        .fillMaxSize()
    ) {

        var scale by remember { mutableStateOf(0.5f) }
        var rotation by remember { mutableStateOf(0f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        val state = rememberTransformableState { zoomChange, offsetChange, rotationChange ->
            scale *= zoomChange
            rotation += rotationChange
            offset += offsetChange
        }

        Box(
            Modifier
                .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
                .graphicsLayer(
                    scaleX = maxOf(.5f, minOf(1f, scale)),
                    scaleY = maxOf(.5f, minOf(1f, scale)),
                    translationX = maxOf(0f, minOf(constraints.maxWidth.toFloat(), offset.x)),
                    translationY = offset.y
                )
                .transformable(state = state)
                .size(maxWidth)
                .alpha(0.5f)
                .background(Color.White)
                .fillMaxSize()
        )
    }
}