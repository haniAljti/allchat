package com.hanialjti.allchat.presentation.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.advancedRectShadow(
    color: Color = Color.Black,
    alpha: Float = 1f,
    cornersRadius: Dp = 0.dp,
    shadowBlurRadius: Dp = 0.dp,
    height: Float? = null,
    width: Float? = null,
    offsetY: Dp = 0.dp,
    offsetX: Dp = 0.dp
) = drawBehind {

    val shadowColor = color.copy(alpha = alpha).toArgb()
    val transparentColor = color.copy(alpha = 0f).toArgb()

    drawIntoCanvas {
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint()
        frameworkPaint.color = transparentColor
        frameworkPaint.setShadowLayer(
            shadowBlurRadius.toPx(),
            offsetX.toPx(),
            offsetY.toPx(),
            shadowColor
        )
        it.drawRoundRect(
            0f,
            0f,
            width ?: this.size.width,
            height ?: this.size.height,
            cornersRadius.toPx(),
            cornersRadius.toPx(),
            paint
        )
    }
}

fun Modifier.drawAccent(
    color: Color,
    cornersRadius: Dp
) = drawBehind {

    val accent = color.toArgb()

    drawIntoCanvas {
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint()
        frameworkPaint.color = accent

        it.drawRoundRect(
            0f,
            0f,
             25f,
            this.size.height,
            cornersRadius.toPx(),
            cornersRadius.toPx(),
            paint
        )
    }
}

fun Modifier.advancedCircleShadow(
    size: Dp,
    color: Color = Color.Black,
    alpha: Float = 1f,
    shadowBlurRadius: Dp = 0.dp,
    radius: Float = 0f,
    offsetY: Dp = 0.dp,
    offsetX: Dp = 0.dp
) = drawBehind {

    val shadowColor = color.copy(alpha = alpha).toArgb()
    val transparentColor = color.copy(alpha = 0f).toArgb()

    drawIntoCanvas {
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint()
        frameworkPaint.color = transparentColor
        frameworkPaint.setShadowLayer(
            shadowBlurRadius.toPx(),
            offsetX.toPx(),
            offsetY.toPx(),
            shadowColor
        )
        it.drawCircle(
            center = Offset(size.toPx() / 2, size.toPx() / 2),
            radius = radius,
            paint = paint
        )
    }
}

fun Modifier.advancedRectShadow(
    color: Color = Color.Black,
    alpha: Float = 1f,
    cornersRadius: Dp = 0.dp,
    shadowBlurRadius: Dp = 0.dp,
    size: Float? = null,
    offsetY: Dp = 0.dp,
    offsetX: Dp = 0.dp
) = advancedRectShadow(
    color,
    alpha,
    cornersRadius,
    shadowBlurRadius,
    size,
    size,
    offsetY,
    offsetX
)