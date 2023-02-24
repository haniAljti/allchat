package com.hanialjti.allchat.presentation.component

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
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
    onSwipe: () -> Unit,
    hiddenContent: @Composable () -> Unit,
    swipableContent: @Composable () -> Unit
) {

    var size by remember { mutableStateOf(Size.Zero) }
    val sizePx = with(LocalDensity.current) { 60.dp.toPx() }
    val layoutDirection = LocalLayoutDirection.current
    val swipeableState = rememberSwipeableState(SwipeState.Original)
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    if (swipeableState.isAnimationRunning) {
        DisposableEffect(Unit) {
            onDispose {
                Logger.d { swipeableState.currentValue.toString() }
                if (swipeableState.currentValue == SwipeState.Left) {
                    scope.launch {
                        swipeableState.animateTo(SwipeState.Original)
                        onSwipe()
                    }
                }
            }
        }
    }

    val anchors = mapOf(0f to SwipeState.Original, sizePx to SwipeState.Left)

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

    Row(modifier = boxModifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.offset {
                IntOffset(swipeableState.offset.value.roundToInt() - 50.dp.roundToPx(), 0)
            }
        ) {
            hiddenContent()
        }
        Box(
            modifier = Modifier.offset {
                IntOffset(swipeableState.offset.value.roundToInt(), 0)
            }
        ) {
            swipableContent()
        }
    }


}

private enum class SwipeState { Left, Original, Right }