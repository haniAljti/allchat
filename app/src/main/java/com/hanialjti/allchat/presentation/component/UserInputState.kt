package com.hanialjti.allchat.presentation.component

import androidx.compose.foundation.interaction.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeableState
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
class UserInputState(
    recordInitialValue: RecordingButtonState,
    scope: CoroutineScope
) {

    val interactionSource by mutableStateOf(MutableInteractionSource())
    val swipeableState by mutableStateOf(SwipeableState(recordInitialValue))
    var isRecording by mutableStateOf(false)

    private val recordSwipeThreshold = 0.5f
    private val swipeProgress get() = swipeableState.progress.fraction
    private val isInInitialValue get() = swipeableState.progress.from == swipeableState.progress.to
    val swipeOffset get() = swipeableState.offset.value
    val recordSwipeValue get() = swipeableState.currentValue
    val thresholdReached by derivedStateOf { !isInInitialValue && swipeProgress > recordSwipeThreshold }

    suspend fun animateRecordingStateTo(state: RecordingButtonState) {
        swipeableState.animateTo(state)
    }

//    companion object {
//        fun Saver() = androidx.compose.runtime.saveable.Saver<UserInputState, RecordingButtonState>(
//            save = { rec },
//            restore = { UserInputState() }
//        )
//    }

    init {
        val pressInteractions = mutableListOf<PressInteraction.Press>()
        val dragInteractions = mutableListOf<DragInteraction.Start>()
        scope.launch {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> pressInteractions.add(interaction)
                    is PressInteraction.Release -> pressInteractions.remove(interaction.press)
                    is PressInteraction.Cancel -> pressInteractions.remove(interaction.press)
                    is DragInteraction.Start -> dragInteractions.add(interaction)
                    is DragInteraction.Stop -> dragInteractions.remove(interaction.start)
                    is DragInteraction.Cancel -> dragInteractions.remove(interaction.start)
                }
                isRecording = (pressInteractions.isNotEmpty() || dragInteractions.isNotEmpty()) && !thresholdReached
            }
        }
    }
}

data class RecordButtonState(
    val size: Dp,
    val color: Color
)

enum class RecordingButtonState { Initial, Cancel }

@Composable
fun rememberUserInputState(
    recordInitialValue: RecordingButtonState,
    scope: CoroutineScope = rememberCoroutineScope()
): UserInputState {
    return remember(
    ) {
        UserInputState(
            recordInitialValue,
            scope
        )
    }
}

