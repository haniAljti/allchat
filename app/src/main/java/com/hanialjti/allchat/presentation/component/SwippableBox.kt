package com.hanialjti.allchat.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.hanialjti.allchat.common.utils.Logger
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SwipeableBox(
    modifier: Modifier = Modifier,
    onLeftSwipe: () -> Unit = {},
    onRightSwipe: () -> Unit = {},
    allowedSwipeDirection: SwipeDirection,
    hiddenContent: @Composable () -> Unit,
    swipableContent: @Composable () -> Unit
) {

    var size by remember { mutableStateOf(Size.Zero) }
    val sizePx = with(LocalDensity.current) { 60.dp.toPx() }
    val layoutDirection = LocalLayoutDirection.current
    val swipeableState = rememberSwipeableState(SwipeState.Original)
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val anchors = mutableMapOf(0f to SwipeState.Original)

    when (allowedSwipeDirection) {
        SwipeDirection.LEFT -> anchors[sizePx] = SwipeState.Left
        SwipeDirection.RIGHT -> anchors[-sizePx] = SwipeState.Right
        SwipeDirection.LEFT_AND_RIGHT -> {
            anchors[sizePx] = SwipeState.Left
            anchors[-sizePx] = SwipeState.Right
        }
    }

    if (swipeableState.isAnimationRunning) {
        DisposableEffect(Unit) {
            onDispose {
                Logger.d { swipeableState.currentValue.toString() }
                when (swipeableState.currentValue) {
                    SwipeState.Left -> scope.launch {
                        swipeableState.animateTo(SwipeState.Original)
                        onLeftSwipe()
                    }
                    SwipeState.Right -> scope.launch {
                        swipeableState.animateTo(SwipeState.Original)
                        onRightSwipe()
                    }
                    else -> {}
                }
            }
        }
    }


    val thresholdReached by remember {
        derivedStateOf {
            swipeableState.progress.from != swipeableState.progress.to
                    && swipeableState.progress.fraction > 0.5f
        }
    }

    if (thresholdReached) {
        LaunchedEffect(Unit) {
            scope.launch {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }

    val boxModifier = modifier
        .onSizeChanged { size = Size(it.width.toFloat(), it.height.toFloat()) }
        .swipeable(
            state = swipeableState,
            anchors = anchors,
            reverseDirection = layoutDirection == LayoutDirection.Rtl,
            thresholds = { _, _ -> FractionalThreshold(0.5f) },
            orientation = Orientation.Horizontal
        )

    Box(modifier = boxModifier) {

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = 10.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            hiddenContent()
        }

        Box(modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .offset { IntOffset(swipeableState.offset.value.roundToInt(), 0) }
        ) {
            swipableContent()
        }

    }

}

private enum class SwipeState { Left, Original, Right }
enum class SwipeDirection { LEFT, RIGHT, LEFT_AND_RIGHT }