package com.hanialjti.allchat.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedCheckBox(
    isChecked: Boolean,
    onClicked: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {

    val transition =
        updateTransition(targetState = isChecked, label = "")

    val recordButtonColor by transition.animateColor(label = "") { recording ->
        if (recording) MaterialTheme.colorScheme.primary else Color.Transparent
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(35))
            .border(
                1.dp,
                MaterialTheme.colorScheme.onBackground,
                RoundedCornerShape(35)
            )
            .background(color = recordButtonColor)
            .clickable {
                onClicked(!isChecked)
            }
    ) {
        AnimatedVisibility(
            visible = isChecked,
            enter = scaleIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
            exit = scaleOut()
        ) {
            Icon(imageVector = Icons.Default.Check, contentDescription = null)
        }
    }
}